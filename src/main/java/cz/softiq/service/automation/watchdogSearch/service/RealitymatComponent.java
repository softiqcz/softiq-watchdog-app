package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.core.configuration.profile.Profile;
import cz.softiq.service.watchdog.entity.WatchDogBuilder;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.pages.search.RealitymatPage;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
class RealitymatComponent {
  private final SearchComponent searchComponent;
  private final WatchDogProviderOfferComponent providerOfferComponent;
  private final WatchDogSyncMode syncMode;

  public WatchDogBuilder doRealitymatSearch(
      WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    if (syncMode.isHtmlMode()
        && StringUtils.isNotEmpty(watchDog.getUrlRealitymat())
        && !watchDog.getDoResetSearch()) {
      watchDogBuilder.setHasOffers(true);
      return watchDogBuilder;
    }
    return new RealitymatPage().doRealitymatSearch(watchDog, watchDogBuilder);
  }

  public void getLatestOffers(WatchDogEntity watchDog, Profile activeProfile) {
    if (activeProfile != Profile.DEV) {
      Set<String> existingOfferIds = searchComponent.getExistingOfferIds(watchDog);
      var newOffers = providerOfferComponent.getLatestRealitymatOffers(watchDog, existingOfferIds);
      searchComponent.saveNewOffers(newOffers);
    } else {
      Set<String> existingOfferIds = new HashSet<>();
      providerOfferComponent.getLatestRealitymatOffers(watchDog, existingOfferIds);
    }
  }
}
