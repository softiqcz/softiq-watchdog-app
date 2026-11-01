package cz.softiq.watchdog;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class WatchdogApplicationTest {
  @Test
  public void distinguishesBootOptionsFromSuitePaths() {
    assertNull(
        WatchdogApplication.resolveSuitePath(new String[] {"--spring.profiles.active=prod"}, null));
    assertEquals(
        WatchdogApplication.resolveSuitePath(new String[] {"watchdog/watchDogSync.xml"}, null),
        "watchdog/watchDogSync.xml");
    assertEquals(
        WatchdogApplication.resolveSuitePath(new String[0], "watchdog/watchDogSyncDaily.xml"),
        "watchdog/watchDogSyncDaily.xml");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void rejectsInvalidSuiteArgument() {
    WatchdogApplication.resolveSuitePath(new String[] {"unknown"}, null);
  }
}
