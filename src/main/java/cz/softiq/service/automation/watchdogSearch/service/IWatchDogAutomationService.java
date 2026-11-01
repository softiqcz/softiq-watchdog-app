package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.service.watchdog.entity.WatchDogBuilder;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.model.enums.MembershipEnum;
import cz.softiq.service.watchdog.model.enums.RealityEnums;

public interface IWatchDogAutomationService
    extends IBeanProvider, IBaseService, IWatchDogProfileProvider {

  default boolean checkIfApplicable(WatchDogEntity watchDog, RealityEnums realityEnum) {
    return getWatchDogAutomationService().checkIfApplicable(watchDog, realityEnum);
  }

  default String setSearch(WatchDogEntity watchDog, MembershipEnum membership) {
    return getWatchDogAutomationService().setSearch(watchDog, membership, getActiveProfile());
  }

  default void saveUrl(
      WatchDogEntity watchDog,
      Boolean isNewRun,
      RealityEnums realityEnum,
      WatchDogBuilder watchDogBuilder) {
    if (isNewRun && watchDogBuilder.getIsChangedUrl()) {
      getWatchDogAutomationService().saveUrl(watchDog, realityEnum);
    }
  }

  default String extractSrealityDetailPrice(String pageSource, String offerId) {
    return getWatchDogAutomationService().extractSrealityDetailPrice(pageSource, offerId);
  }

  private WatchDogAutomationService getWatchDogAutomationService() {
    return getBean(WatchDogAutomationService.class);
  }
}
