package cz.softiq.watchdog;

import static org.testng.Assert.*;

import java.nio.file.Files;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testng.annotations.Test;

public class WatchdogConfigurationTest {
  @Test
  public void loadsWatchdogConfigAndMapsEnvironmentFileWithoutStartingApplication()
      throws Exception {
    var envFile = Files.createTempFile("watchdog-config-test-", ".env");
    try {
      Files.writeString(
          envFile, "SOFTIQ_JWT_KEY=isolated-jwt-fixture\nSOFTIQ_DB_URL=jdbc:unused:test\n");
      new ApplicationContextRunner()
          .withPropertyValues(
              "spring.profiles.active=dev",
              "spring.config.import=" + envFile.toUri() + "[.properties]")
          .withInitializer(new ConfigDataApplicationContextInitializer())
          .run(
              context -> {
                assertNull(context.getStartupFailure());
                var environment = context.getEnvironment();
                assertEquals(environment.getProperty("secret.jwt.key"), "isolated-jwt-fixture");
                assertEquals(environment.getProperty("spring.datasource.url"), "jdbc:unused:test");
                assertNotNull(environment.getProperty("softiq.watchdog.runner.image"));
                assertTrue(
                    java.util.Arrays.asList(environment.getActiveProfiles()).contains("dev"));
              });
    } finally {
      Files.deleteIfExists(envFile);
    }
  }
}
