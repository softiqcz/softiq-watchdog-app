package cz.softiq.tests.base;

import cz.softiq.common.service.test.ITestService;
import cz.softiq.common.service.testng.ITestNGService;
import cz.softiq.core.service.ICoreSoftiqService;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogService;
import cz.softiq.service.softiq.service.email.IEmailService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.*;

@SpringBootTest(classes = cz.softiq.watchdog.WatchdogApplication.class)
@TestExecutionListeners(mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class MainApiTestClass extends AbstractTestNGSpringContextTests
    implements IWatchDogService, ICoreSoftiqService, IEmailService, ITestService, ITestNGService {

  @Value("${spring.mail.username}")
  protected String adminEmail;

  protected static List<String> exceptions = TestSuiteReportState.EXCEPTIONS;
  protected static List<String> successMessages = TestSuiteReportState.SUCCESS_MESSAGES;
  protected String suiteName;
  protected LocalDateTime suiteStartedAt;
  protected int exceptionCountBeforeMethod;
  protected int successCountBeforeMethod;
  private static final String SOFTIQ_SUITE_NAME = "SoftiqSuite";

  @BeforeSuite
  public void beforeSuite(ITestContext context) {
    TestSuiteReportState.reset(context.getSuite().getName());
    this.suiteName = TestSuiteReportState.suiteName;
    this.suiteStartedAt = TestSuiteReportState.suiteStartedAt;
  }

  @BeforeMethod(alwaysRun = true)
  public void rememberExceptionCount() {
    this.exceptionCountBeforeMethod = exceptions.size();
    this.successCountBeforeMethod = successMessages.size();
  }

  protected void executeWithFailureMessage(Supplier<String> test, String failureMessage) {
    int errorSizeBefore = exceptions.size();
    executeWithExceptionHandling(test, exceptions, successMessages);
    if (replaceNewErrorsWithFailureMessage(errorSizeBefore, failureMessage) && isSoftiqSuite()) {
      throw new AssertionError(toPlainFailureMessage(exceptions.getLast()));
    }
  }

  protected boolean replaceNewErrorsWithFailureMessage(int errorSizeBefore, String failureMessage) {
    if (exceptions.size() == errorSizeBefore) {
      return false;
    }

    List<String> errorDetails =
        new ArrayList<>(exceptions.subList(errorSizeBefore, exceptions.size()));
    exceptions.subList(errorSizeBefore, exceptions.size()).clear();
    exceptions.add(createFailureParagraph(failureMessage, errorDetails));
    return true;
  }

  private String createFailureParagraph(String failureMessage, List<String> errorDetails) {
    String detail =
        errorDetails.stream()
            .map(this::stripParagraphTags)
            .filter(message -> !message.isBlank())
            .reduce((first, second) -> first + "<br>" + second)
            .orElse("");

    String detailHtml = detail.isBlank() ? "" : String.format("<br><span>%s</span>", detail);
    return String.format(
        "<p style=\"color: red; font-weight: bold;\">%s%s</p>", failureMessage, detailHtml);
  }

  private String stripParagraphTags(String message) {
    if (message == null) {
      return "";
    }

    return message.replaceAll("(?i)</?p[^>]*>", "").trim();
  }

  private boolean isSoftiqSuite() {
    return SOFTIQ_SUITE_NAME.equals(TestSuiteReportState.suiteName);
  }

  private String toPlainFailureMessage(String message) {
    return stripParagraphTags(message)
        .replaceAll("(?i)</?span[^>]*>", "")
        .replaceAll("(?i)<br\\s*/?>", "\n");
  }

  @AfterMethod(alwaysRun = true)
  public void recordTestResult(ITestResult result) {
    if (result.getStatus() == ITestResult.FAILURE
        && exceptions.size() == exceptionCountBeforeMethod) {
      exceptions.add(createTestFailureMessage(result));
      return;
    }

    if (result.getStatus() == ITestResult.SUCCESS
        && successMessages.size() == successCountBeforeMethod
        && exceptions.size() == exceptionCountBeforeMethod) {
      successMessages.add(String.format("<p>%s passed</p>", result.getMethod().getMethodName()));
    }
  }

  private String createTestFailureMessage(ITestResult result) {
    Throwable throwable = result.getThrowable();
    String errorMessage = throwable == null ? "" : throwable.getMessage();
    String errorDetail = errorMessage == null || errorMessage.isBlank() ? "" : " - " + errorMessage;
    return String.format(
        "<p style=\"color: red; font-weight: bold;\">%s FAILED%s</p>",
        result.getMethod().getMethodName(), errorDetail);
  }

  @AfterSuite(alwaysRun = true)
  public void report() {
    if (!TestSuiteReportState.markReportSent()) {
      return;
    }

    this.suiteName = TestSuiteReportState.suiteName;
    this.suiteStartedAt = TestSuiteReportState.suiteStartedAt;
    testSuiteReport(
        buildTestReport(
            suiteName,
            suiteStartedAt,
            adminEmail,
            getActiveProfile().name(),
            successMessages,
            exceptions));
  }
}
