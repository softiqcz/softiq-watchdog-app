package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.core.configuration.profile.Profile;
import cz.softiq.core.helper.ReportBuilder;
import cz.softiq.service.watchdog.entity.WatchDogBuilder;
import cz.softiq.service.watchdog.entity.WatchDogEntity;

public interface ISearchService extends IBeanProvider, IBaseService {

  private SrealityService getSrealityService() {
    return getBean(SrealityService.class);
  }

  default WatchDogBuilder doSrealitySearch(
      WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    return getSrealityService().doSrealitySearch(watchDog, watchDogBuilder);
  }

  default void getSrealityOffers(WatchDogEntity watchDog, Profile activeProfile) {
    getSrealityService().getLatestOffers(watchDog, activeProfile);
  }

  default ReportBuilder syncCssSro() {
    return getSrealityService().syncCssSro();
  }

  default ReportBuilder syncPronajemSnadno() {
    return getSrealityService().syncPronajemSnadno();
  }

  private BezrealitkyService getBezrealitkyService() {
    return getBean(BezrealitkyService.class);
  }

  default WatchDogBuilder doBezrealitkySearch(
      WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    return getBezrealitkyService().doBezrealitkySearch(watchDog, watchDogBuilder);
  }

  default void getBezrealitkyOffers(WatchDogEntity watchDog, Profile activeProfile) {
    getBezrealitkyService().getLatestOffers(watchDog, activeProfile);
  }

  private BazosService getBazosService() {
    return getBean(BazosService.class);
  }

  default WatchDogBuilder doBazosSearch(WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    return getBazosService().doBazosSearch(watchDog, watchDogBuilder);
  }

  default void getBazosOffers(
      WatchDogEntity watchDog, Profile activeProfile, WatchDogBuilder watchDogBuilder) {
    getBazosService().getLatestOffers(watchDog, activeProfile, watchDogBuilder);
  }

  private IdnesService getIdnesService() {
    return getBean(IdnesService.class);
  }

  default WatchDogBuilder doIdnesSearch(WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    return getIdnesService().doIdnesSearch(watchDog, watchDogBuilder);
  }

  default void getIdnesOffers(WatchDogEntity watchDog, Profile activeProfile) {
    getIdnesService().getLatestOffers(watchDog, activeProfile);
  }

  private RealitymatService getRealitymatService() {
    return getBean(RealitymatService.class);
  }

  default WatchDogBuilder doRealitymatSearch(
      WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    return getRealitymatService().doRealitymatSearch(watchDog, watchDogBuilder);
  }

  default void getRealitymatOffers(WatchDogEntity watchDog, Profile activeProfile) {
    getRealitymatService().getLatestOffers(watchDog, activeProfile);
  }

  private CeskeRealityService getCeskeRealityService() {
    return getBean(CeskeRealityService.class);
  }

  default WatchDogBuilder doCeskeRealitySearch(
      WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    return getCeskeRealityService().doCeskeRealitySearch(watchDog, watchDogBuilder);
  }

  default void getCeskeRealityOffers(WatchDogEntity watchDog, Profile activeProfile) {
    getCeskeRealityService().getLatestOffers(watchDog, activeProfile);
  }
}
