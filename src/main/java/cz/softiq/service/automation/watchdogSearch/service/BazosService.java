package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.core.configuration.profile.Profile;
import cz.softiq.service.watchdog.entity.WatchDogBuilder;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class BazosService {
  private final BazosComponent component;

  WatchDogBuilder doBazosSearch(WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    return component.doBazosSearch(watchDog, watchDogBuilder);
  }

  void getLatestOffers(
      WatchDogEntity watchDog, Profile activeProfile, WatchDogBuilder watchDogBuilder) {
    component.getLatestOffers(watchDog, activeProfile, watchDogBuilder);
  }
}
