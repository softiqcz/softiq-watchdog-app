package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.core.configuration.profile.Profile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WatchDogReportingService {
  private final WatchDogReportingComponent reportingComponent;

  void reportWatchDogs(Boolean isDailyRun, Boolean isNonstopRun, Profile activeProfile) {
    reportingComponent.reportWatchDogs(isDailyRun, isNonstopRun, activeProfile);
  }
}
