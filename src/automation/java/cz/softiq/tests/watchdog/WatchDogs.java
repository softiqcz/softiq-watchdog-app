package cz.softiq.tests.watchdog;

import static com.codeborne.selenide.Selenide.sleep;
import static cz.softiq.core.configuration.profile.Profile.DEV;
import static cz.softiq.service.watchdog.model.enums.MembershipEnum.STANDARD;

import cz.softiq.service.automation.watchdogSearch.service.IWatchDogService;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.model.enums.SettingsEnums;
import cz.softiq.tests.base.MainWatchdogTestClass;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class WatchDogs extends MainWatchdogTestClass implements IWatchDogService {
  private static final Logger log = LoggerFactory.getLogger(WatchDogs.class);
  private static final String WATCHDOG_FAILURE_MARKER = "Watchdog search failed";

  @Test
  @Parameters({"IsDailyRun", "isWeeklyRun"})
  // will run every 2h
  public void runWatchDogV2(Boolean isDailyRun, Boolean isWeeklyRun) {
    List<WatchDogEntity> watchDogs = new ArrayList<>();
    if (getActiveProfile().equals(DEV)) {
      watchDogs.addAll(getAllNewWatchDogs());
      watchDogs.addAll(getAllWatchDogsToReset());
      //            watchDogs.add(getWatchDogById(843));
    } else {
      if (isWeeklyRun) {
        watchDogs.addAll(getAllEnabledWatchDogs(SettingsEnums.FrequencyEnum.WEEKLY));
      } else {
        watchDogs.addAll(getAllEnabledWatchDogs(SettingsEnums.FrequencyEnum.OFTEN));
        watchDogs.addAll(getAllEnabledWatchDogs(SettingsEnums.FrequencyEnum.DAILY));
      }
    }

    log.info(
        "Selected {} watchdogs for sync in profile {}: {}",
        watchDogs.size(),
        getActiveProfile(),
        watchDogs.stream()
            .map(watchDog -> watchDog.getId() + "=" + watchDog.getRealityUrl())
            .toList());

    for (WatchDogEntity watchDog : watchDogs) {
      executeWatchDogSearch(watchDog);
      includeScreenshotOnFailure();
    }
    if (!isWeeklyRun) {
      reportWatchDogsV2(isDailyRun, Boolean.FALSE);
    }
  }

  @Test
  public void runWatchDogNonstop() {
    // getAllEnabledWatchDogs NONSTOP
    log.info("Running nonstop watchdogs");
    int errorCount = 0;
    int successCount = 0;
    for (int i = 0; i < 10; i++) {
      sleep(60000);
      List<WatchDogEntity> watchDogs = new ArrayList<>();
      watchDogs.addAll(getAllEnabledWatchDogs(SettingsEnums.FrequencyEnum.NONSTOP));
      watchDogs.addAll(getAllWatchDogsToReset());
      watchDogs.addAll(getAllNewWatchDogs());
      for (WatchDogEntity watchDog : watchDogs) {
        executeWatchDogSearch(watchDog);
        includeScreenshotOnFailure();
        reportWatchDogsV2(Boolean.FALSE, Boolean.TRUE);
      }

      errorCount = exceptions.size();
      successCount = successMessages.size();
      int totalRuns = errorCount + successCount;
      if (totalRuns > 0 && (double) errorCount / totalRuns > 0.5) {
        break;
      }
    }
  }

  private void executeWatchDogSearch(WatchDogEntity watchDog) {
    if (getActiveProfile().equals(DEV)) {
      handleWatchDogResult(watchDog, setSearch(watchDog, STANDARD));
      return;
    }

    try {
      handleWatchDogResult(watchDog, setSearch(watchDog, STANDARD));
    } catch (Throwable e) {
      log.error(
          "Watchdog {} {} {} failed",
          watchDog.getId(),
          watchDog.getExactAddress(),
          watchDog.getClient().getClientName(),
          e);
      exceptions.add(
          String.format(
              "Watchdog %d %s %s FAILED%s",
              watchDog.getId(),
              watchDog.getExactAddress(),
              watchDog.getClient().getClientName(),
              formatExceptionMessage(e.getMessage())));
    }
  }

  private void handleWatchDogResult(WatchDogEntity watchDog, String result) {
    if (isFailedWatchDogResult(result)) {
      exceptions.add(result);
    } else if (result != null && !result.isBlank()) {
      successMessages.add(result);
    } else {
      exceptions.add(
          String.format(
              "<p style=\"color: red; font-weight: bold;\">Watchdog %d %s %s FAILED - empty result</p>",
              watchDog.getId(), watchDog.getExactAddress(), watchDog.getClient().getClientName()));
    }
  }

  private boolean isFailedWatchDogResult(String result) {
    return result != null && result.contains(WATCHDOG_FAILURE_MARKER);
  }

  private String formatExceptionMessage(String errorMessage) {
    return errorMessage == null || errorMessage.isBlank() ? "" : " - " + errorMessage;
  }
}
