package cz.softiq.common.service.docker;

import static cz.softiq.service.watchdog.model.enums.SettingsEnums.FrequencyEnum.*;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.testExecution.CoreDockerService;
import cz.softiq.service.watchdog.model.enums.SettingsEnums;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class DockerComponent extends CoreDockerService implements IBaseService {
  @Value("${softiq.watchdog.runner.image:softiq-watchdog-app:4.1.0}")
  private String imageName;

  @Value("${softiq.watchdog.runner.env-file:/app/config/watchdog.env}")
  private String runnerEnvFile;

  private String imageNameMakon = "makon-app-2.0";

  @Value("${softiq.docker.screenshot.host-dir:/home/svemar/MyApps/softiq/build/libs/screenshots}")
  private String screenshotHostDir;

  @Value("${softiq.test.screenshot.base-url:http://svemar04.local:8081}")
  private String screenshotBaseUrl;

  public void runMorningSuite() {
    runAsDockerContainerWithScreenshots(
        "run-in-morning-suite-v2", "clients/runInMorningSuite.xml", imageName);
  }

  public void softiqSuite() {
    runAsDockerContainerWithScreenshots("softiq-suite-v2", "softiq/softiqSuite.xml", imageName);
  }

  public void runWatchDog(SettingsEnums.FrequencyEnum frequencyEnum) {
    if (DAILY.equals(frequencyEnum)) {
      runAsDockerContainerWithScreenshots(
          "watchdog-sync-daily-v2", "watchdog/watchDogSyncDaily.xml", imageName);
    } else if (WEEKLY.equals(frequencyEnum)) {
      runAsDockerContainerWithScreenshots(
          "watchdog-sync-weekly-v2", "watchdog/watchDogSyncWeekly.xml", imageName);
    } else if (OFTEN.equals(frequencyEnum)) {
      runAsDockerContainerWithScreenshots(
          "watchdog-sync-often-v2", "watchdog/watchDogSync.xml", imageName);
    }
  }

  public void runEveningSuite() {
    runAsDockerContainer("makon-agents-sync-v2", "runAgents.xml", imageNameMakon);
  }

  private void runAsDockerContainerWithScreenshots(
      String containerName, String xmlPath, String imageName) {
    String command =
        "mkdir -p "
            + screenshotHostDir
            + " && docker stop "
            + containerName
            + " || true && docker rm "
            + containerName
            + " || true && docker run -d --name "
            + containerName
            + " --env-file "
            + runnerEnvFile
            + " -e TZ=Europe/Prague -e DISPLAY=:99"
            + " -e SPRING_PROFILE_ACTIVE=prod"
            + " -e MODE_SELECTOR=html"
            + " -e SOFTIQ_TEST_SCREENSHOT_DIR=/app/build/screenshots"
            + " -e SOFTIQ_TEST_SCREENSHOT_BASE_URL="
            + screenshotBaseUrl
            + " -v /tmp/.X11-unix:/tmp/.X11-unix"
            + " --ipc=host --cap-add=SYS_ADMIN"
            + " -v /var/run/docker.sock:/var/run/docker.sock"
            + " -v "
            + screenshotHostDir
            + ":/app/build/screenshots"
            + " "
            + imageName
            + " bash -c 'Xvfb :99 -screen 0 1920x1080x24 & export DISPLAY=:99 && java -Dsoftiq.test.screenshot.dir=/app/build/screenshots -jar /app/app.jar "
            + xmlPath
            + "'";

    try {
      Process process = new ProcessBuilder("sh", "-c", command).inheritIO().start();
      int exitCode = process.waitFor();
      if (exitCode == 0) {
        log.info("Docker container {} started successfully!", containerName);
      } else {
        log.info("Docker container {} failed to start! Exit code: {}", containerName, exitCode);
      }
    } catch (Exception e) {
      log.error("Error starting Docker container:", e);
    }
  }
}
