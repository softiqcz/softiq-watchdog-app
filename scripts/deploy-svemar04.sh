#!/usr/bin/env bash
set -euo pipefail
version="${1:?Gradle project version is required}"
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
remote='svemar@svemar04.local'
remote_dir='MyApps/softiq-watchdog-v2'
ssh "$remote" "mkdir -p $remote_dir/build/libs"
rsync -avz "$project_dir/build/libs/softiq-watchdog-app-${version}.jar" "$remote:$remote_dir/build/libs/"
rsync -avz "$project_dir/Dockerfile" "$remote:$remote_dir/"
ssh "$remote" bash -s -- "${version%-RELEASE}" <<'REMOTE'
set -euo pipefail
export PATH="/usr/local/bin:/opt/homebrew/bin:/Applications/Docker.app/Contents/Resources/bin:$PATH"
cd "$HOME/MyApps/softiq-watchdog-v2"
test -f watchdog.env || { echo 'Create watchdog.env before deployment; see README.' >&2; exit 1; }
image="softiq-watchdog-app-v2:$1"
screenshots="$HOME/MyApps/softiq-watchdog-v2/screenshots"
mkdir -p "$screenshots"
# Refuse name collisions; never stop or replace an existing container.
for container in softiq-watchdog-app-v2 watchdog-sync-professional-v2 softiq-test-screenshots-v2 watchdog-sync-often-v2 watchdog-sync-daily-v2 watchdog-sync-weekly-v2 run-in-morning-suite-v2 softiq-suite-v2 makon-agents-sync-v2; do
  if docker container inspect "$container" >/dev/null 2>&1; then
    echo "Container $container already exists; deployment aborted without replacing it." >&2
    exit 1
  fi
done
docker build -t "$image" .
docker run -d --name softiq-watchdog-app-v2 --restart unless-stopped \
  --env-file watchdog.env -e TZ=Europe/Prague -e SPRING_PROFILE_ACTIVE=prod,prod-scheduled \
  -e SOFTIQ_WATCHDOG_RUNNER_IMAGE="$image" -e SOFTIQ_DOCKER_SCREENSHOT_HOST_DIR="$screenshots" \
  -e SOFTIQ_TEST_SCREENSHOT_BASE_URL=http://svemar04.local:8082 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v "$PWD/watchdog.env:/app/config/watchdog.env:ro" \
  -v "$screenshots:/app/build/screenshots" "$image"
docker run -d --name watchdog-sync-professional-v2 --restart unless-stopped \
  --env-file watchdog.env -e TZ=Europe/Prague -e SPRING_PROFILE_ACTIVE=prod \
  -e MODE_SELECTOR=html -e SOFTIQ_TEST_SCREENSHOT_DIR=/app/build/screenshots \
  -e SOFTIQ_TEST_SCREENSHOT_BASE_URL=http://svemar04.local:8082 \
  -v "$screenshots:/app/build/screenshots" --ipc=host \
  "$image" bash -c 'Xvfb :99 -screen 0 1920x1080x24 & export DISPLAY=:99; exec java -jar /app/app.jar watchdog/watchDogSyncProfessional.xml'
docker run -d --name softiq-test-screenshots-v2 --restart unless-stopped \
  -v "$screenshots:/usr/share/nginx/html:ro" -p 8082:80 nginx:alpine
REMOTE
