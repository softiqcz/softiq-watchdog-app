package cz.softiq.common.service.test;

import cz.softiq.core.model.TestReport;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class TestService {
  private final TestComponent component;

  TestReport buildTestReport(
      String suiteName,
      LocalDateTime suiteStartedAt,
      String adminEmail,
      String profile,
      List<String> successMessages,
      List<String> exceptions) {
    return component.buildTestReport(
        suiteName, suiteStartedAt, adminEmail, profile, successMessages, exceptions);
  }
}
