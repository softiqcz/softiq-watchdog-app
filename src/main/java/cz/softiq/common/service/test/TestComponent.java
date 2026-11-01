package cz.softiq.common.service.test;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.model.TestReport;
import cz.softiq.core.model.enums.ResultEnum;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class TestComponent implements IBaseService {

  public TestReport buildTestReport(
      String suiteName,
      LocalDateTime suiteStartedAt,
      String adminEmail,
      String profile,
      List<String> successMessages,
      List<String> exceptions) {
    LocalDateTime suiteFinishedAt = LocalDateTime.now();
    ResultEnum result = exceptions.isEmpty() ? ResultEnum.PASSED : ResultEnum.FAILED;

    return TestReport.builder()
        .name(suiteName)
        .emailTo(adminEmail)
        .result(result)
        .startedAt(suiteStartedAt)
        .finishedAt(suiteFinishedAt)
        .duration(formatDuration(suiteStartedAt, suiteFinishedAt))
        .durationSeconds(getDurationSeconds(suiteStartedAt, suiteFinishedAt))
        .profile(profile)
        .successCount(successMessages.size())
        .errorCount(exceptions.size())
        .totalCount(successMessages.size() + exceptions.size())
        .successMessages(new ArrayList<>(successMessages))
        .errorMessages(new ArrayList<>(exceptions))
        .build();
  }

  private String formatDuration(LocalDateTime startedAt, LocalDateTime finishedAt) {
    if (startedAt == null || finishedAt == null) {
      return "-";
    }

    Duration duration = Duration.between(startedAt, finishedAt);
    long totalSeconds = Math.max(duration.getSeconds(), 0);
    long hours = totalSeconds / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long seconds = totalSeconds % 60;

    if (hours > 0) {
      return String.format("%dh %02dm %02ds", hours, minutes, seconds);
    }
    if (minutes > 0) {
      return String.format("%dm %02ds", minutes, seconds);
    }
    return String.format("%ds", seconds);
  }

  private long getDurationSeconds(LocalDateTime startedAt, LocalDateTime finishedAt) {
    if (startedAt == null || finishedAt == null) {
      return 0;
    }

    return Math.max(Duration.between(startedAt, finishedAt).getSeconds(), 0);
  }
}
