# softiq-watchdog-app

Automation and scheduling application for `svemar04.local`. Java 24, Spring Boot 3.5.3, Gradle 9. Uses `cz.softiq.watchdog:softiq-watchdog-core:1.0.0` and the same database as the production API.

## Build

```sh
./gradlew -PwatchdogCoreDir=../softiq-watchdog-core check bootJar
```

Omit source substitution after publishing core. Configure `GITHUB_USERNAME` and `GITHUB_TOKEN` for both private package repositories, or use Gradle properties `github.username` / `github.token`. `-PuseMavenLocal` is supported after publishing core locally.

`check` runs isolated parser, startup and scheduling-registration tests. It does not execute operational suites. Those live under `src/automation` and are included only in the watchdog Boot JAR, `build/libs/softiq-watchdog-app-4.1.0-RELEASE.jar`. `shadowJar` aliases `bootJar`.

## Run modes

Scheduler (default profiles `prod,prod-scheduled`, no HTTP server):

```sh
java -jar build/libs/softiq-watchdog-app-4.1.0-RELEASE.jar
```

Direct suite execution:

```sh
java -jar build/libs/softiq-watchdog-app-4.1.0-RELEASE.jar watchdog/watchDogSyncDaily.xml
```

`TEST_FILE` is also supported. Suite startup forces the `prod` profile, independent of the scheduler environment; `-Dsoftiq.suite.profile=dev` is available for development. A suite profile containing `prod-scheduled` is rejected. Existing schedules, time zones and disabled jobs remain unchanged. The professional runner remains a separate container using the same image.

API checks in `softiq/softiqSuite.xml` call the production API over HTTP, rather than embedding controllers. They require explicit `SOFTIQ_API_BASE_URL`, `SOFTIQ_API_TEST_EMAIL`, and `SOFTIQ_API_TEST_PASSWORD`. These are operational checks and can change data/send emails, as before; they are never part of `check`.

## Configuration and deployment

Copy `.env.example` to `~/MyApps/softiq-watchdog-v2/watchdog.env` on svemar04 and fill in the existing installation values. Keep credentials outside Git/images. The scheduler mounts this file read-only and passes its values to spawned suite containers using the Docker CLI. Standard Spring environment overrides are also supported.

Important paths/configuration:

- `SOFTIQ_WATCHDOG_RUNNER_IMAGE`: defaults to `softiq-watchdog-app:4.1.0`.
- `softiq.watchdog.runner.env-file`: defaults to `/app/config/watchdog.env`, as mounted by deployment.
- `SOFTIQ_DOCKER_SCREENSHOT_HOST_DIR`: path on the Docker host; deployment uses `~/MyApps/softiq-watchdog-v2/screenshots`.
- `SOFTIQ_TEST_SCREENSHOT_DIR`: `/app/build/screenshots` inside runner containers.
- `SOFTIQ_TEST_SCREENSHOT_BASE_URL`: deployment overrides the application default to `http://svemar04.local:8082`.

The watchdog Docker image contains Java, Chromium/ChromeDriver, Xvfb and Docker CLI. The API image needs none of the browser tooling. Docker must be available on svemar04; the scheduler uses its mounted Docker socket to launch suite containers.

```sh
./gradlew -PwatchdogCoreDir=../softiq-watchdog-core upload-to-svemar04
```

The deploy task builds/checks, uploads only the watchdog artifact, then starts scheduler, professional runner and screenshot server. Deployment uses `softiq-watchdog-app-v2`, `watchdog-sync-professional-v2`, and `softiq-test-screenshots-v2`; scheduled suite containers also end in `-v2`. It never stops/removes existing containers and aborts if a v2 name already exists. The image is `softiq-watchdog-app-v2:<version>`, files live in `~/MyApps/softiq-watchdog-v2`, and screenshots use port 8082. Both installations still share the database, so running both schedulers can process the same work and send duplicate reports. See `softiq-app/docs/watchdog-split.md` for cutover/rollback. No remote deployment occurs during ordinary builds.

## IntelliJ TestNG runs

Reload the Gradle project after source-set changes. Choose `softiq-watchdog-app.automation` as the TestNG classpath module and select the suite from `src/automation/resources`, not `build/resources`. Set the working directory to the watchdog project root. Keep `MODE_SELECTOR=gui` and `SPRING_PROFILE_ACTIVE=dev` for local GUI development.

Watchdog configuration lives under `src/main/resources/config/` so Spring Boot gives it precedence over the old core library's root `application.properties`. It automatically imports the project-root `.env` as properties. A separate `SPRING_CONFIG_IMPORT` run variable is unnecessary when the working directory is correct. The automation source set declares a dependency on main output, including its configuration resources, for IDE imports as well as Gradle builds.
