package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.core.configuration.profile.Profile;
import cz.softiq.service.watchdog.entity.WatchDogBuilder;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import cz.softiq.service.watchdog.pages.search.CeskeRealityPage;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class CeskeRealityService {
  private final CeskeRealityComponent component;

  WatchDogBuilder doCeskeRealitySearch(WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    return component.doCeskeRealitySearch(watchDog, watchDogBuilder);
  }

  void getLatestOffers(WatchDogEntity watchDog, Profile activeProfile) {
    component.getLatestOffers(watchDog, activeProfile);
  }

  List<WatchDogEstatesEntity> getLatestCeskeRealityOffers(
      WatchDogEntity watchDog, Set<String> existingOfferIds) {
    return component.getLatestCeskeRealityOffers(watchDog, existingOfferIds);
  }

  CeskeRealityPage createCeskeRealityPage() {
    return component.createCeskeRealityPage();
  }
}
