package cz.softiq.common.service.docker;

import static cz.softiq.service.watchdog.model.enums.SettingsEnums.FrequencyEnum.*;

import cz.softiq.service.watchdog.model.enums.SettingsEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class DockerService {
  private final DockerComponent component;

  void runMorningSuite() {
    component.runMorningSuite();
  }

  void softiqSuite() {
    component.softiqSuite();
  }

  void runWatchDog(SettingsEnums.FrequencyEnum frequencyEnum) {
    component.runWatchDog(frequencyEnum);
  }

  void runEveningSuite() {
    component.runEveningSuite();
  }
}
