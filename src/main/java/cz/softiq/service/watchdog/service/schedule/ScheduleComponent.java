package cz.softiq.service.watchdog.service.schedule;

import static cz.softiq.service.watchdog.model.enums.SettingsEnums.FrequencyEnum.*;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.common.service.docker.IDockerService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.core.service.ICoreSoftiqService;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogMaintenanceService;
import cz.softiq.service.realiq.service.portfolio.IPortfolioRecordService;
import cz.softiq.service.softiq.service.blog.IBlogService;
import cz.softiq.service.softiq.service.email.IEmailService;
import cz.softiq.service.softiq.service.reporting.IReportingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class ScheduleComponent
    implements IWatchDogMaintenanceService,
        IDockerService,
        IReportingService,
        IBeanProvider,
        ICoreSoftiqService,
        IBaseService,
        IPortfolioRecordService,
        IEmailService,
        IBlogService {

  public void runMorningSuiteScheduledService() {
    log.info("Starting runMorningSuiteScheduledService");
    runMorningSuite();
  }

  public void runSoftiqSuiteScheduledService() {
    log.info("Starting softiqSuite");
    //    softiqSuite();
  }

  public void checkPaymentStillLiveScheduledService() {
    log.info("Starting checkPaymentStillLiveScheduledService");
    checkPaymentStillLive();
  }

  public void notifyBasicUsersAboutEndOfMembershipScheduledService() {
    log.info("Starting notifyBasicUsersAboutEndOfMembershipScheduledService");
    notifyBasicUsersAboutEndOfMembership();
  }

  public void deleteInActiveAccountsScheduledService() {
    log.info("Starting deleteInActiveAccountsScheduledService");
    deleteInActiveAccounts(); // delete inactive, unpaid accounts
    deleteAccountScheduled(); // delete pending accounts
    deleteOldWatchdogChildrenEstates(); // delete all watchdog children estates
  }

  public void runWatchDogOftenScheduledService() {
    log.info("Starting runWatchDogOftenScheduledService");
    runWatchDog(OFTEN);
  }

  public void runWatchDogDailyScheduledService() {
    log.info("Starting runWatchDogDailyScheduledService");
    runWatchDog(DAILY);
  }

  public void runWatchDogWeeklyScheduledService() {
    log.info("Starting runWatchDogWeeklyScheduledService");
    runWatchDog(WEEKLY);
  }

  public void checkActivity() {
    noActivityMail();
  }

  public void sendPortfolioRecordRemindersScheduledService() {
    log.info("Starting sendPortfolioRecordRemindersScheduledService");
    sendDueRecordReminders();
  }

  public void sendPromoEmailsScheduledService() {
    log.info("Starting sendPromoEmailsScheduledService");
    sendEligiblePromoEmails();
  }

  public void deleteOldTestReportsScheduledService() {
    log.info("Starting deleteOldTestReportsScheduledService");
    long deletedCount = deleteOldTestReports(7);
    log.info("Deleted {} old test reports", deletedCount);
  }

  public void sendMonthlyWatchdogSummariesScheduledService() {
    log.info("Starting sendMonthlyWatchdogSummariesScheduledService");
    sendMonthlyWatchdogSummaries();
  }

  public void sendMonthlyPortfolioSummariesScheduledService() {
    log.info("Starting sendMonthlyPortfolioSummariesScheduledService");
    sendMonthlyPortfolioSummaries();
  }

  public void publishDueBlogPostsScheduledService() {
    publishDueBlogPosts();
  }

  //  @Scheduled(cron = "0 30 18 * * 6")
  public void runMakon() {
    runEveningSuite();
  }
}
