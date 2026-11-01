package cz.softiq.watchdog;

import cz.softiq.core.testExecution.CoreTestNGExecution;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(
    basePackages = {"cz.softiq.common", "cz.softiq.service", "cz.softiq.core"},
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.ANNOTATION,
          classes = {
            org.springframework.stereotype.Controller.class,
            org.springframework.web.bind.annotation.RestController.class
          }),
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = {
            "cz\\.softiq\\.core\\.CoreRepositoryConfig",
            "cz\\.softiq\\.core\\.sitemap\\..*",
            "cz\\.softiq\\.core\\.security\\.(CoreCustomUserDetailsService|SafeRequestFilter|jwt\\.CoreJwtAuthFilter)"
          })
    })
@EnableJpaRepositories(basePackages = "cz.softiq")
@EntityScan(basePackages = "cz.softiq")
public class WatchdogApplication {
  public static void main(String[] args) {
    String suitePath = resolveSuitePath(args, System.getenv("TEST_FILE"));
    if (suitePath != null) {
      String profile = System.getProperty("softiq.suite.profile", "prod");
      if (profile.contains("prod-scheduled")) {
        throw new IllegalArgumentException("Suite processes cannot enable prod-scheduled");
      }
      System.setProperty("spring.profiles.active", profile);
      CoreTestNGExecution execution = new CoreTestNGExecution();
      execution.cleanupArtifacts();
      execution.runTestNg(suitePath);
      return;
    }
    SpringApplication app = new SpringApplication(WatchdogApplication.class);
    app.setWebApplicationType(WebApplicationType.NONE);
    app.run(args);
  }

  static String resolveSuitePath(String[] args, String testFile) {
    String path = args.length > 0 && !args[0].startsWith("--") ? args[0] : testFile;
    if (path == null || path.isBlank()) return null;
    if (!path.endsWith(".xml"))
      throw new IllegalArgumentException("Expected a TestNG XML suite path");
    return path;
  }
}
