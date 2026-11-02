package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.service.watchdog.entity.WatchDogEntity;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

public final class SrealitySearchAddress {
  private SrealitySearchAddress() {}

  public static void validate(String title, WatchDogEntity watchDog) {
    String expected = normalize(watchDog.getSearchAddress());
    if (expected.isBlank()) {
      throw new IllegalStateException(
          "Missing expected Sreality search address for watchdog " + watchDog.getId());
    }
    String actual = normalize(title);
    if (actual.isBlank() || !(" " + actual + " ").contains(" " + expected + " ")) {
      throw new IllegalStateException(
          "Sreality search title mismatch for watchdog "
              + watchDog.getId()
              + ": expected address '"
              + watchDog.getSearchAddress()
              + "', but title was '"
              + title
              + "'");
    }
  }

  private static String normalize(String text) {
    return StringUtils.stripAccents(StringUtils.defaultString(text))
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", " ")
        .trim();
  }
}
