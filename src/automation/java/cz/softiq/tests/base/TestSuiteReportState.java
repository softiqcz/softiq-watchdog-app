package cz.softiq.tests.base;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

final class TestSuiteReportState {
  static final List<String> EXCEPTIONS = new ArrayList<>();
  static final List<String> SUCCESS_MESSAGES = new ArrayList<>();

  static String suiteName;
  static LocalDateTime suiteStartedAt;

  private static final AtomicBoolean REPORT_SENT = new AtomicBoolean(false);

  private TestSuiteReportState() {}

  static void reset(String name) {
    EXCEPTIONS.clear();
    SUCCESS_MESSAGES.clear();
    suiteName = name;
    suiteStartedAt = LocalDateTime.now();
    REPORT_SENT.set(false);
  }

  static boolean markReportSent() {
    return REPORT_SENT.compareAndSet(false, true);
  }
}
