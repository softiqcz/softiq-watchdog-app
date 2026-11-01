package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;

public interface IWatchDogReportingService
    extends IBeanProvider, IBaseService, IWatchDogProfileProvider {

  default void reportWatchDogsV2(Boolean isDailyRun, Boolean isNonstopRun) {
    getWatchDogReportingService().reportWatchDogs(isDailyRun, isNonstopRun, getActiveProfile());
  }

  private WatchDogReportingService getWatchDogReportingService() {
    return getBean(WatchDogReportingService.class);
  }
}
