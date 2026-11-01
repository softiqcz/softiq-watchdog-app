package cz.softiq.service.automation.watchdogSearch.service;

import static cz.softiq.core.configuration.profile.Profile.DEV;

import cz.softiq.core.configuration.profile.Profile;
import cz.softiq.service.watchdog.entity.WatchDogBuilder;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import cz.softiq.service.watchdog.pages.search.CeskeRealityPage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CeskeRealityComponent implements IWatchDogAutomationService {
  private final SearchComponent searchComponent;
  private final WatchDogHtmlOfferParser htmlOfferParser;
  private final WatchDogSyncMode syncMode;

  public WatchDogBuilder doCeskeRealitySearch(
      WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    if (syncMode.isHtmlMode()
        && StringUtils.isNotEmpty(watchDog.getUrlCeskereality())
        && !watchDog.getDoResetSearch()) {
      watchDogBuilder.setHasOffers(true);
      return watchDogBuilder;
    }
    return createCeskeRealityPage().doCeskeRealitySearch(watchDog, watchDogBuilder);
  }

  public void getLatestOffers(WatchDogEntity watchDog, Profile activeProfile) {
    if (activeProfile != DEV) {
      Set<String> existingOfferIds = searchComponent.getExistingOfferIds(watchDog);
      var newOffers = getLatestCeskeRealityOffers(watchDog, existingOfferIds);
      searchComponent.saveNewOffers(newOffers);
    } else {
      Set<String> existingOfferIds = new HashSet<>();
      getLatestCeskeRealityOffers(watchDog, existingOfferIds);
    }
  }

  public List<WatchDogEstatesEntity> getLatestCeskeRealityOffers(
      WatchDogEntity watchDog, Set<String> existingOfferIds) {
    //    CeskeRealityPage page = createCeskeRealityPage();
    if (syncMode.isHtmlMode() && StringUtils.isNotEmpty(watchDog.getUrlCeskereality())) {
      try {
        //        return htmlOfferParser.getCeskeRealityOffers(watchDog, existingOfferIds);
      } catch (RuntimeException htmlFailure) {
        log.warn(
            "Watchdog {} Ceskereality HTML sync failed; retrying with GUI",
            watchDog.getId(),
            htmlFailure);
        //        page.doCeskeRealitySearch(
        //            watchDog, WatchDogBuilder.builder().watchDogEntity(watchDog).build());
      }
    }

    //    var offers = page.getLatestOffers(existingOfferIds, watchDog);
    return null;
  }

  public CeskeRealityPage createCeskeRealityPage() {
    return new CeskeRealityPage();
  }
}
