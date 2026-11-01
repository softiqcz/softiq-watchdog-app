package cz.softiq.service.watchdog.service.schedule;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;

public interface IScheduleService extends IBeanProvider, IBaseService {

  private ScheduleService getScheduleService() {
    return getBean(ScheduleService.class);
  }

  default void runMorningSuiteScheduledService() {
    getScheduleService().runMorningSuiteScheduledService();
  }

  default void runSoftiqSuiteScheduledService() {
    getScheduleService().runSoftiqSuiteScheduledService();
  }

  default void checkPaymentStillLiveScheduledService() {
    getScheduleService().checkPaymentStillLiveScheduledService();
  }

  default void notifyBasicUsersAboutEndOfMembershipScheduledService() {
    getScheduleService().notifyBasicUsersAboutEndOfMembershipScheduledService();
  }

  default void deleteInActiveAccountsScheduledService() {
    getScheduleService().deleteInActiveAccountsScheduledService();
  }

  default void runWatchDogOftenScheduledService() {
    getScheduleService().runWatchDogOftenScheduledService();
  }

  default void runWatchDogDailyScheduledService() {
    getScheduleService().runWatchDogDailyScheduledService();
  }

  default void runWatchDogWeeklyScheduledService() {
    getScheduleService().runWatchDogWeeklyScheduledService();
  }

  default void checkActivity() {
    getScheduleService().checkActivity();
  }

  default void sendPortfolioRecordRemindersScheduledService() {
    getScheduleService().sendPortfolioRecordRemindersScheduledService();
  }

  default void sendPromoEmailsScheduledService() {
    getScheduleService().sendPromoEmailsScheduledService();
  }

  default void deleteOldTestReportsScheduledService() {
    getScheduleService().deleteOldTestReportsScheduledService();
  }

  default void runMakon() {
    getScheduleService().runMakon();
  }
}
