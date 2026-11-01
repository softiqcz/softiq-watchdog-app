package cz.softiq.service.automation.watchdogSearch.service;

import static cz.softiq.service.softiq.model.constants.MessageConstants.*;
import static cz.softiq.service.watchdog.model.enums.RealityEnums.*;

import com.codeborne.selenide.WebDriverRunner;
import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.core.configuration.profile.Profile;
import cz.softiq.service.softiq.service.reporting.IReportingService;
import cz.softiq.service.watchdog.component.WatchDogManagementComponent;
import cz.softiq.service.watchdog.entity.*;
import cz.softiq.service.watchdog.model.builder.WatchDogReportBuilder;
import cz.softiq.service.watchdog.model.enums.*;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
class WatchDogAutomationComponent implements IReportingService {
  private final WatchDogManagementComponent watchDogManagementComponent;
  private final WatchDogPersistenceComponent watchDogPersistenceComponent;
  private final SearchComponent searchComponent;
  private final WatchDogHtmlOfferParser htmlOfferParser;

  public String extractSrealityDetailPrice(String pageSource, String offerId) {
    return htmlOfferParser.extractSrealityDetailPrice(pageSource, offerId);
  }

  public String setSearch(
      WatchDogEntity watchDog, MembershipEnum membership, Profile activeProfile) {
    log.info(
        "Starting " + watchDog.getSearchAddress() + " " + watchDog.getClient().getClientName());
    WatchDogReportBuilder report = searchComponent.doSearch(watchDog, membership, activeProfile);
    return handleReporting(report, watchDog);
  }

  public void saveUrl(WatchDogEntity watchdog, RealityEnums realityEnums) {
    String currentUrl = WebDriverRunner.url();

    Map<RealityEnums, Function<WatchDogEntity, String>> urlGetters =
        Map.of(
            RealityEnums.SREALITY,
            WatchDogEntity::getUrlSreality,
            RealityEnums.BAZOS,
            WatchDogEntity::getUrlBazos,
            RealityEnums.BEZREALITKY,
            WatchDogEntity::getUrlBezrealitky,
            RealityEnums.REALITYMAT,
            WatchDogEntity::getUrlRealitymat,
            CESKEREALITY,
            WatchDogEntity::getUrlCeskereality,
            RealityEnums.IDNES,
            WatchDogEntity::getUrlIdnesReality);

    Map<RealityEnums, Consumer<String>> urlSetters =
        Map.of(
            RealityEnums.SREALITY,
            watchdog::setUrlSreality,
            RealityEnums.BAZOS,
            watchdog::setUrlBazos,
            RealityEnums.BEZREALITKY,
            watchdog::setUrlBezrealitky,
            RealityEnums.REALITYMAT,
            watchdog::setUrlRealitymat,
            CESKEREALITY,
            watchdog::setUrlCeskereality,
            RealityEnums.IDNES,
            watchdog::setUrlIdnesReality);

    Function<WatchDogEntity, String> urlGetter = urlGetters.get(realityEnums);
    Consumer<String> urlSetter = urlSetters.get(realityEnums);

    if (urlGetter == null || urlSetter == null) {
      throw new IllegalArgumentException("Unknown reality enum: " + realityEnums);
    }

    Map<RealityEnums, String> expectedHosts =
        Map.of(
            RealityEnums.SREALITY,
            "sreality.cz",
            RealityEnums.BAZOS,
            "bazos.cz",
            RealityEnums.BEZREALITKY,
            "bezrealitky.cz",
            RealityEnums.REALITYMAT,
            "realitymat.cz",
            CESKEREALITY,
            "ceskereality.cz",
            RealityEnums.IDNES,
            "idnes.cz");
    String currentHost = URI.create(currentUrl).getHost();
    String expectedHost = expectedHosts.get(realityEnums);
    String normalizedHost = currentHost == null ? "" : currentHost.toLowerCase(Locale.ROOT);
    boolean isExpectedHost =
        normalizedHost.equals(expectedHost) || normalizedHost.endsWith("." + expectedHost);
    if (!isExpectedHost) {
      throw new SoftiqException(
          "Refusing to save URL for watchdog "
              + watchdog.getId()
              + ": expected "
              + realityEnums.getName()
              + " host '"
              + expectedHost
              + "', but browser is on '"
              + currentUrl
              + "'");
    }

    urlSetter.accept(currentUrl);
    watchDogPersistenceComponent.saveWatchDog(watchdog);
  }

  public boolean checkIfApplicable(WatchDogEntity watchDog, RealityEnums realityEnum) {
    boolean applicable = true;
    switch (realityEnum) {
      case BAZOS:
        if (watchDog.getEstateCondition() != null
            || watchDog.getFlatCategory() != null
            || hasFilterValue(watchDog.getUsableAreaFrom())
            || hasFilterValue(watchDog.getUsableAreaTo())
            || hasFilterValue(watchDog.getFloorFrom())
            || hasFilterValue(watchDog.getFloorTo())) {
          log.info("Cannot setup filters here, removing {}", BAZOS.name());
          String newRealityUrl = watchDog.getRealityUrl().replace("Bazos;", "");
          watchDog.setRealityUrl(newRealityUrl);
          watchDog.setUrlBazos(null);
          watchDogPersistenceComponent.saveWatchDog(watchDog);
          applicable = false;
        }
        break;
      case REALITYMAT:
        if (hasFilterValue(watchDog.getFlatCategory()) && watchDog.getEstateType().equals("Domy")) {
          log.info("Cannot setup filters here, removing {}", REALITYMAT.name());
          String newRealityUrl = watchDog.getRealityUrl().replace("Realitymat;", "");
          watchDog.setRealityUrl(newRealityUrl);
          watchDog.setUrlRealitymat(null);
          watchDogPersistenceComponent.saveWatchDog(watchDog);
          applicable = false;
        }
        break;
      case CESKEREALITY:
        if ((hasFilterValue(watchDog.getPriceFrom())
                || hasFilterValue(watchDog.getPriceTo())
                || hasFilterValue(watchDog.getUsableAreaFrom())
                || hasFilterValue(watchDog.getUsableAreaTo())
                || hasFilterValue(watchDog.getFloorFrom())
                || hasFilterValue(watchDog.getFloorTo()))
            || (hasFilterValue(watchDog.getFlatCategory())
                && watchDog.getEstateType().equals("Domy"))) {
          log.info("Cannot setup filters here, removing {}", CESKEREALITY.name());
          String newRealityUrl = watchDog.getRealityUrl().replace("Ceskereality", "");
          watchDog.setRealityUrl(newRealityUrl);
          watchDog.setUrlCeskereality(null);
          watchDogPersistenceComponent.saveWatchDog(watchDog);
          applicable = false;
        }
        break;
    }
    return applicable;
  }

  private boolean hasFilterValue(String value) {
    return value != null
        && !"0".equals(value)
        && !"null".equalsIgnoreCase(value)
        && !value.isBlank();
  }
}
