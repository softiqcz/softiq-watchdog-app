package cz.softiq.common.service.test;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.core.model.TestReport;
import java.time.LocalDateTime;
import java.util.List;

public interface ITestService extends IBeanProvider, IBaseService {

  default TestReport buildTestReport(
      String suiteName,
      LocalDateTime suiteStartedAt,
      String adminEmail,
      String profile,
      List<String> successMessages,
      List<String> exceptions) {
    return getTestService()
        .buildTestReport(
            suiteName, suiteStartedAt, adminEmail, profile, successMessages, exceptions);
  }

  private TestService getTestService() {
    return getBean(TestService.class);
  }
}
