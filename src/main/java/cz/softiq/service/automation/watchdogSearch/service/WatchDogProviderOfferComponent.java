package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.service.watchdog.entity.WatchDogBuilder;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import cz.softiq.service.watchdog.pages.search.BazosPage;
import cz.softiq.service.watchdog.pages.search.BezrealitkyPage;
import cz.softiq.service.watchdog.pages.search.IdnesPage;
import cz.softiq.service.watchdog.pages.search.RealitymatPage;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class WatchDogProviderOfferComponent {
  private final WatchDogHtmlOfferParser htmlOfferParser;
  private final WatchDogSyncMode syncMode;

  public List<WatchDogEstatesEntity> getLatestBazosOffers(
      WatchDogEntity watchDog, Set<String> existingOfferIds, WatchDogBuilder watchDogBuilder) {
    if (syncMode.isHtmlMode() && StringUtils.isNotEmpty(watchDog.getUrlBazos())) {
      return htmlOfferParser.getBazosOffers(watchDog, existingOfferIds);
    }
    return new BazosPage().getLatestOffers(existingOfferIds, watchDog, watchDogBuilder);
  }

  public List<WatchDogEstatesEntity> getLatestBezrealitkyOffers(
      WatchDogEntity watchDog, Set<String> existingOfferIds) {
    if (syncMode.isHtmlMode() && StringUtils.isNotEmpty(watchDog.getUrlBezrealitky())) {
      return htmlOfferParser.getBezrealitkyOffers(watchDog, existingOfferIds);
    }
    return new BezrealitkyPage().getLatestOffers(existingOfferIds, watchDog);
  }

  public List<WatchDogEstatesEntity> getLatestIdnesOffers(
      WatchDogEntity watchDog, Set<String> existingOfferIds) {
    if (syncMode.isHtmlMode() && StringUtils.isNotEmpty(watchDog.getUrlIdnesReality())) {
      return htmlOfferParser.getIdnesOffers(watchDog, existingOfferIds);
    }
    return new IdnesPage().getLatestOffers(existingOfferIds, watchDog);
  }

  public List<WatchDogEstatesEntity> getLatestRealitymatOffers(
      WatchDogEntity watchDog, Set<String> existingOfferIds) {
    if (syncMode.isHtmlMode() && StringUtils.isNotEmpty(watchDog.getUrlRealitymat())) {
      return htmlOfferParser.getRealitymatOffers(watchDog, existingOfferIds);
    }
    return new RealitymatPage().getLatestOffers(existingOfferIds, watchDog);
  }
}
