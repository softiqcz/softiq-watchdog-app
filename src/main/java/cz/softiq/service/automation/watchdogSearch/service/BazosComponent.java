package cz.softiq.service.automation.watchdogSearch.service;

import static com.codeborne.selenide.Selenide.open;
import static cz.softiq.core.configuration.profile.Profile.DEV;

import cz.softiq.core.configuration.profile.Profile;
import cz.softiq.service.watchdog.entity.WatchDogBuilder;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.model.enums.RealityEnums;
import cz.softiq.service.watchdog.pages.search.BazosPage;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
class BazosComponent implements IWatchDogAutomationService {
  private final SearchComponent searchComponent;
  private final WatchDogProviderOfferComponent providerOfferComponent;
  private final WatchDogSyncMode syncMode;

  public WatchDogBuilder doBazosSearch(WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    if (StringUtils.isNotEmpty(watchDog.getUrlBazos()) && !watchDog.getDoResetSearch()) {
      if (!syncMode.isHtmlMode()) {
        open(watchDog.getUrlBazos());
      }
      return watchDogBuilder;
    }

    if ((StringUtils.isEmpty(watchDog.getUrlBazos()) || watchDog.getDoResetSearch())) {
      String baseUrl = "https://reality.bazos.cz/";

      Map<String, String> offerTypeMap =
          Map.of(
              "Prodej", "prodam/",
              "Pronájem", "pronajmu/");

      Map<String, String> estateTypeMap =
          Map.of(
              "Byty", "byt/",
              "Domy", "dum/",
              "Pozemky", "pozemek/",
              "Komerční", "prostory/");

      String searchUrl =
          baseUrl
              + offerTypeMap.getOrDefault(watchDog.getOfferType(), "")
              + estateTypeMap.getOrDefault(watchDog.getEstateType(), "");

      StringJoiner queryParams = new StringJoiner("&", "?", "");

      queryParams.add("hledat=");
      queryParams.add("rubriky=reality");
      queryParams.add("hlokalita=");

      int inRangeInt =
          Optional.ofNullable(watchDog.getInRange())
              .map(range -> range.replaceAll(",", ".").replaceAll("[^0-9.]", ""))
              .filter(range -> !range.isEmpty())
              .map(Double::parseDouble)
              .map(Math::round)
              .map(Long::intValue)
              .orElse(0);

      if (watchDog.getExactAddress().equals("Praha")) {
        inRangeInt = 10;
      }

      queryParams.add("humkreis=" + inRangeInt);

      queryParams.add("cenaod=" + Optional.ofNullable(watchDog.getPriceFrom()).orElse(""));
      queryParams.add("cenado=" + Optional.ofNullable(watchDog.getPriceTo()).orElse(""));

      queryParams.add("Submit=Hledat");
      queryParams.add("order=");
      queryParams.add("crp=");
      queryParams.add("kitx=ano");

      searchUrl += queryParams;
      new BazosPage().doSearch(searchUrl, watchDog);

      watchDogBuilder.setIsChangedUrl(true);
      saveUrl(watchDog, true, RealityEnums.BAZOS, watchDogBuilder);
    }
    return watchDogBuilder;
  }

  public void getLatestOffers(
      WatchDogEntity watchDog, Profile activeProfile, WatchDogBuilder watchDogBuilder) {
    if (activeProfile != DEV) {
      Set<String> existingOfferIds = searchComponent.getExistingOfferIds(watchDog);
      var newOffers =
          providerOfferComponent.getLatestBazosOffers(watchDog, existingOfferIds, watchDogBuilder);
      searchComponent.saveNewOffers(newOffers);
    } else {
      Set<String> existingOfferIds = new HashSet<>();
      providerOfferComponent.getLatestBazosOffers(watchDog, existingOfferIds, watchDogBuilder);
    }
  }
}
