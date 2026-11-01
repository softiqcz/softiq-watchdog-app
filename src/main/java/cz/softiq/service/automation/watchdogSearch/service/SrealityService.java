package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.core.configuration.profile.Profile;
import cz.softiq.core.helper.ReportBuilder;
import cz.softiq.service.watchdog.entity.WatchDogBuilder;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class SrealityService {
  private final SrealityComponent component;

  WatchDogBuilder doSrealitySearch(WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    return component.doSrealitySearch(watchDog, watchDogBuilder);
  }

  void getLatestOffers(WatchDogEntity watchDog, Profile activeProfile) {
    component.getLatestOffers(watchDog, activeProfile);
  }

  ReportBuilder syncPronajemSnadno() {
    return component.syncPronajemSnadno();
  }

  ReportBuilder syncCssSro() {
    return component.syncCssSro();
  }
}
