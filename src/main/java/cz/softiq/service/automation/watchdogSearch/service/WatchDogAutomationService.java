package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.core.configuration.profile.Profile;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.model.enums.MembershipEnum;
import cz.softiq.service.watchdog.model.enums.RealityEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WatchDogAutomationService {
  private final WatchDogAutomationComponent automationComponent;

  String extractSrealityDetailPrice(String pageSource, String offerId) {
    return automationComponent.extractSrealityDetailPrice(pageSource, offerId);
  }

  boolean checkIfApplicable(WatchDogEntity watchDog, RealityEnums realityEnum) {
    return automationComponent.checkIfApplicable(watchDog, realityEnum);
  }

  String setSearch(WatchDogEntity watchDog, MembershipEnum membership, Profile activeProfile) {
    return automationComponent.setSearch(watchDog, membership, activeProfile);
  }

  void saveUrl(WatchDogEntity watchDog, RealityEnums realityEnum) {
    automationComponent.saveUrl(watchDog, realityEnum);
  }
}
