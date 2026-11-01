package cz.softiq.service.watchdog.service.schedule;

import static cz.softiq.service.watchdog.model.enums.SettingsEnums.FrequencyEnum.*;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@org.springframework.context.annotation.Profile("prod-scheduled")
@Service
@RequiredArgsConstructor
class ScheduleService {
  private final ScheduleComponent component;

  @Scheduled(cron = "0 10 8 * * *")
  @Scheduled(cron = "0 05 14 * * *")
  @Scheduled(cron = "0 35 19 * * *")
  @Scheduled(cron = "0 30 21 * * *")
  void runMorningSuiteScheduledService() {
    component.runMorningSuiteScheduledService();
  }

  @Scheduled(cron = "0 17 18 * * *")
  void runSoftiqSuiteScheduledService() {
    component.runSoftiqSuiteScheduledService();
  }

  @Scheduled(cron = "0 15 09 * * *")
  void checkPaymentStillLiveScheduledService() {
    component.checkPaymentStillLiveScheduledService();
  }

  @Scheduled(cron = "0 55 11 * * *")
  void notifyBasicUsersAboutEndOfMembershipScheduledService() {
    component.notifyBasicUsersAboutEndOfMembershipScheduledService();
  }

  @Scheduled(cron = "0 50 19 * * *")
  void deleteInActiveAccountsScheduledService() {
    component.deleteInActiveAccountsScheduledService();
  }

  @Scheduled(cron = "0 0 */2 * * *")
  @Scheduled(cron = "0 30 19 * * *")
  @Scheduled(cron = "0 40 21 * * *")
  void runWatchDogOftenScheduledService() {
    component.runWatchDogOftenScheduledService();
  }

  @Scheduled(cron = "0 10 16 * * *")
  void runWatchDogDailyScheduledService() {
    component.runWatchDogDailyScheduledService();
  }

  @Scheduled(cron = "0 30 16 * * *")
  @Scheduled(cron = "0 25 21 * * *")
  void runWatchDogWeeklyScheduledService() {
    component.runWatchDogWeeklyScheduledService();
  }

  @Scheduled(cron = "0 10 10 * * *")
  void checkActivity() {
    component.checkActivity();
  }

  @Scheduled(cron = "0 0 8 * * *", zone = "Europe/Prague")
  void sendPortfolioRecordRemindersScheduledService() {
    component.sendPortfolioRecordRemindersScheduledService();
  }

  @Scheduled(cron = "0 40 08 * * 6", zone = "Europe/Prague")
  void sendPromoEmailsScheduledService() {
    component.sendPromoEmailsScheduledService();
  }

  @Scheduled(cron = "0 30 10 * * *")
  void deleteOldTestReportsScheduledService() {
    component.deleteOldTestReportsScheduledService();
  }

  @Scheduled(cron = "0 0 9 L * *", zone = "Europe/Prague")
  void sendMonthlyWatchdogSummariesScheduledService() {
    component.sendMonthlyWatchdogSummariesScheduledService();
  }

  @Scheduled(cron = "0 5 9 1 * *", zone = "Europe/Prague")
  void sendMonthlyPortfolioSummariesScheduledService() {
    component.sendMonthlyPortfolioSummariesScheduledService();
  }

  @Scheduled(cron = "0 15 9 * * *", zone = "Europe/Prague")
  void publishDueBlogPostsScheduledService() {
    component.publishDueBlogPostsScheduledService();
  }

  void runMakon() {
    component.runMakon();
  }
}
