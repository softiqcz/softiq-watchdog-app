package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.core.configuration.profile.Profile;
import cz.softiq.service.watchdog.entity.WatchDogBuilder;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class BezrealitkyService {
  private final BezrealitkyComponent component;

  WatchDogBuilder doBezrealitkySearch(WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    return component.doBezrealitkySearch(watchDog, watchDogBuilder);
  }

  void getLatestOffers(WatchDogEntity watchDog, Profile activeProfile) {
    component.getLatestOffers(watchDog, activeProfile);
  }
}
