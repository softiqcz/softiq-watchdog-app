package cz.softiq.service.automation.watchdogSearch.service;

import static com.codeborne.selenide.Selenide.*;
import static cz.softiq.core.configuration.profile.Profile.DEV;
import static cz.softiq.service.watchdog.model.enums.RealityEnums.IDNES;

import cz.softiq.core.configuration.profile.Profile;
import cz.softiq.service.watchdog.entity.WatchDogBuilder;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.pages.search.IdnesPage;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
class IdnesComponent implements IWatchDogAutomationService {
  private final SearchComponent searchComponent;
  private final WatchDogProviderOfferComponent providerOfferComponent;
  private final WatchDogSyncMode syncMode;

  public WatchDogBuilder doIdnesSearch(WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    if (StringUtils.isNotEmpty(watchDog.getUrlIdnesReality()) && !watchDog.getDoResetSearch()) {
      if (!syncMode.isHtmlMode()) {
        open(watchDog.getUrlIdnesReality());
      }
      return watchDogBuilder;
    }

    if (StringUtils.isEmpty(watchDog.getUrlIdnesReality()) || watchDog.getDoResetSearch()) {
      open("https://reality.idnes.cz");
      new IdnesPage().doSearch(watchDog);
      watchDogBuilder.setIsChangedUrl(true);
      saveUrl(watchDog, true, IDNES, watchDogBuilder);
    }
    return watchDogBuilder;
  }

  public void getLatestOffers(WatchDogEntity watchDog, Profile activeProfile) {
    if (activeProfile != DEV) {
      Set<String> existingOfferIds = searchComponent.getExistingOfferIds(watchDog);
      var newOffers = providerOfferComponent.getLatestIdnesOffers(watchDog, existingOfferIds);
      searchComponent.saveNewOffers(newOffers);
    } else {
      Set<String> existingOfferIds = new HashSet<>();
      providerOfferComponent.getLatestIdnesOffers(watchDog, existingOfferIds);
    }
  }
}
