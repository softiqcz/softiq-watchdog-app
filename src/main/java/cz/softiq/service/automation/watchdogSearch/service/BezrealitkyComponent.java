package cz.softiq.service.automation.watchdogSearch.service;

import static cz.softiq.core.configuration.profile.Profile.DEV;

import cz.softiq.core.configuration.profile.Profile;
import cz.softiq.service.watchdog.entity.WatchDogBuilder;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.pages.search.BezrealitkyPage;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BezrealitkyComponent {
  private final SearchComponent searchComponent;
  private final WatchDogProviderOfferComponent providerOfferComponent;
  private final WatchDogSyncMode syncMode;

  public WatchDogBuilder doBezrealitkySearch(
      WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    if (syncMode.isHtmlMode()
        && StringUtils.isNotEmpty(watchDog.getUrlBezrealitky())
        && !watchDog.getDoResetSearch()) {
      return watchDogBuilder;
    }
    return new BezrealitkyPage().doBezrealitkySearch(watchDog, watchDogBuilder);
  }

  public void getLatestOffers(WatchDogEntity watchDog, Profile activeProfile) {
    if (activeProfile != DEV) {
      Set<String> existingOfferIds = searchComponent.getExistingOfferIds(watchDog);
      var newOffers = providerOfferComponent.getLatestBezrealitkyOffers(watchDog, existingOfferIds);
      searchComponent.saveNewOffers(newOffers);
    } else {
      Set<String> existingOfferIds = new HashSet<>();
      providerOfferComponent.getLatestBezrealitkyOffers(watchDog, existingOfferIds);
    }
  }
}
