package cz.softiq.common.service.docker;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.core.service.ICoreSoftiqService;
import cz.softiq.service.watchdog.model.enums.SettingsEnums;

public interface IDockerService extends IBeanProvider, ICoreSoftiqService, IBaseService {

  private DockerService getDockerService() {
    return getBean(DockerService.class);
  }

  default void runMorningSuite() {
    getDockerService().runMorningSuite();
  }

  default void runWatchDog(SettingsEnums.FrequencyEnum frequencyEnum) {
    getDockerService().runWatchDog(frequencyEnum);
  }

  default void runEveningSuite() {
    getDockerService().runEveningSuite();
  }
}
