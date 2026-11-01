package cz.softiq.service.automation.watchdogSearch.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class WatchDogSyncMode {
  private final String modeSelector;

  WatchDogSyncMode(@Value("${MODE_SELECTOR:gui}") String modeSelector) {
    this.modeSelector = modeSelector;
  }

  public boolean isHtmlMode() {
    return "html".equalsIgnoreCase(StringUtils.trimToEmpty(modeSelector));
  }
}
