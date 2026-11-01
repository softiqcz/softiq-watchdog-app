package cz.softiq.service.automation.watchdogSearch.service;

import static com.codeborne.selenide.Selenide.*;

import cz.softiq.core.configuration.profile.Profile;
import cz.softiq.service.watchdog.entity.WatchDogBuilder;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class IdnesService {
  private final IdnesComponent component;

  WatchDogBuilder doIdnesSearch(WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    return component.doIdnesSearch(watchDog, watchDogBuilder);
  }

  void getLatestOffers(WatchDogEntity watchDog, Profile activeProfile) {
    component.getLatestOffers(watchDog, activeProfile);
  }
}
