package cz.softiq.isolation;

import static org.testng.Assert.*;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testng.annotations.Test;

public class WatchdogIsolationTest {
  @Test
  public void contextLoadsWithIsolatedDatabaseAndMail() {
    runner()
        .run(
            context -> {
              assertNull(context.getStartupFailure(), "Application context must start");
              assertTrue(context.containsBean("watchDogSettingsService"));
              assertTrue(context.containsBean("searchComponent"));
              assertTrue(context.containsBean("dockerService"));
              assertFalse(context.containsBean("watchDogV2Controller"));
              assertFalse(context.containsBean("scheduleService"));
            });
  }

  @Test
  public void schedulerRegistersJobsWithoutExecutingThem() {
    runner()
        .withPropertyValues("spring.profiles.active=prod,prod-scheduled")
        .withBean(
            "taskScheduler",
            org.springframework.scheduling.TaskScheduler.class,
            () -> org.mockito.Mockito.mock(org.springframework.scheduling.TaskScheduler.class))
        .run(
            context -> {
              assertNull(context.getStartupFailure());
              assertTrue(context.containsBean("scheduleService"));
              var processor =
                  context.getBean(
                      org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor
                          .class);
              assertEquals(processor.getScheduledTasks().size(), 21);
              assertFalse(context.containsBean("watchDogV2Controller"));
            });
  }

  private ApplicationContextRunner runner() {
    return new ApplicationContextRunner()
        .withUserConfiguration(cz.softiq.watchdog.WatchdogApplication.class)
        .withPropertyValues(
            "spring.profiles.active=test",
            "spring.main.allow-bean-definition-overriding=true",
            "spring.datasource.url=jdbc:h2:mem:split;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.hikari.maximum-pool-size=1",
            "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
            "spring.jpa.hibernate.ddl-auto=none",
            "spring.jpa.show-sql=false",
            "spring.mail.host=localhost",
            "spring.mail.port=2525",
            "spring.mail.username=test@example.invalid",
            "spring.mail.password=test",
            "secret.jwt.key=01234567890123456789012345678901",
            "secret.automation.password=test",
            "recaptcha.secret=test",
            "gopay.url=http://localhost:1",
            "gopay.client.id=test",
            "gopay.client.secret=test",
            "gopay.go.id=test",
            "softiq.api.base-url=http://localhost:1",
            "softiq.api.test.email=",
            "softiq.api.test.password=");
  }
}
