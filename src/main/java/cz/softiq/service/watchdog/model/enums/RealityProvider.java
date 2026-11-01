package cz.softiq.service.watchdog.model.enums;

import static cz.softiq.service.watchdog.model.enums.RealityEnums.*;

import cz.softiq.core.configuration.profile.Profile;
import cz.softiq.service.automation.watchdogSearch.service.ISearchService;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogService;
import cz.softiq.service.watchdog.entity.WatchDogBuilder;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.model.builder.WatchDogReportBuilder;
import java.util.function.Function;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RealityProvider implements ISearchService, IWatchDogService {
  SREALITY_LOGIC("Sreality", WatchDogEntity::getUrlSreality) {
    @Override
    public void process(
        WatchDogEntity watchDog,
        WatchDogBuilder watchDogBuilder,
        MembershipEnum membership,
        boolean isNewRun,
        Profile activeProfile) {
      watchDogBuilder = doSrealitySearch(watchDog, watchDogBuilder);
      getSrealityOffers(watchDog, activeProfile);
    }

    @Override
    public void handleError(WatchDogReportBuilder report, String errorMessage) {
      report.setDidSrealityFail(true);
      report.setErrorMessageSreality("Sreality-" + errorMessage);
    }
  },
  BEZREALITKY_LOGIC("Bezrealitky", WatchDogEntity::getUrlBezrealitky) {
    @Override
    public void process(
        WatchDogEntity watchDog,
        WatchDogBuilder watchDogBuilder,
        MembershipEnum membership,
        boolean isNewRun,
        Profile activeProfile) {
      watchDogBuilder = doBezrealitkySearch(watchDog, watchDogBuilder);
      if (watchDogBuilder.getIsSettingsSupported()) {
        getBezrealitkyOffers(watchDog, activeProfile);
      }
    }

    @Override
    public void handleError(WatchDogReportBuilder report, String errorMessage) {
      report.setDidBezrealitkyFail(true);
      report.setErrorMessageBezrealitky("Bezrealitky-" + errorMessage);
    }
  },
  BAZOS_LOGIC("Bazos", WatchDogEntity::getUrlBazos) {
    @Override
    public void process(
        WatchDogEntity watchDog,
        WatchDogBuilder watchDogBuilder,
        MembershipEnum membership,
        boolean isNewRun,
        Profile activeProfile) {
      boolean isApplicable = checkIfApplicable(watchDog, RealityEnums.BAZOS);
      if (isApplicable) {
        watchDogBuilder = doBazosSearch(watchDog, watchDogBuilder);
        getBazosOffers(watchDog, activeProfile, watchDogBuilder);
      }
    }

    @Override
    public void handleError(WatchDogReportBuilder report, String errorMessage) {
      report.setDidBazosFail(true);
      report.setErrorMessageBazos("Bazos-" + errorMessage);
    }
  },
  IDNES_LOGIC("Idnes", WatchDogEntity::getUrlIdnesReality) {
    @Override
    public void process(
        WatchDogEntity watchDog,
        WatchDogBuilder watchDogBuilder,
        MembershipEnum membership,
        boolean isNewRun,
        Profile activeProfile) {
      watchDogBuilder = doIdnesSearch(watchDog, watchDogBuilder);
      getIdnesOffers(watchDog, activeProfile);
    }

    @Override
    public void handleError(WatchDogReportBuilder report, String errorMessage) {
      report.setDidIdnesFail(true);
      report.setErrorMessageIdnes("Idnes-" + errorMessage);
    }
  },
  REALITYMAT_LOGIC("Realitymat", WatchDogEntity::getUrlRealitymat) {
    @Override
    public void process(
        WatchDogEntity watchDog,
        WatchDogBuilder watchDogBuilder,
        MembershipEnum membership,
        boolean isNewRun,
        Profile activeProfile) {
      boolean isApplicable = checkIfApplicable(watchDog, RealityEnums.REALITYMAT);
      if (isApplicable) {
        watchDogBuilder = doRealitymatSearch(watchDog, watchDogBuilder);
        if (watchDogBuilder.getHasOffers()) {
          getRealitymatOffers(watchDog, activeProfile);
        }
      }
    }

    @Override
    public void handleError(WatchDogReportBuilder report, String errorMessage) {
      report.setDidRealitymatFail(true);
      report.setErrorMessageRealitymat("Realitymat-" + errorMessage);
    }
  },
  CESKEREALITY_LOGIC("Ceskereality", WatchDogEntity::getUrlCeskereality) {
    @Override
    public void process(
        WatchDogEntity watchDog,
        WatchDogBuilder watchDogBuilder,
        MembershipEnum membership,
        boolean isNewRun,
        Profile activeProfile) {
      boolean isApplicable = checkIfApplicable(watchDog, CESKEREALITY);
      if (isApplicable) {
        watchDogBuilder = doCeskeRealitySearch(watchDog, watchDogBuilder);
        if (watchDogBuilder.getHasOffers() && watchDogBuilder.getIsSettingsSupported()) {
          getCeskeRealityOffers(watchDog, activeProfile);
        }
      }
    }

    @Override
    public void handleError(WatchDogReportBuilder report, String errorMessage) {
      report.setDidCeskeRealityFail(true);
      report.setErrorMessageCeskeReality("Ceskereality-" + errorMessage);
    }
  };

  private final String name;
  private final Function<WatchDogEntity, String> originUrlGetter;

  public String getOriginUrl(WatchDogEntity watchDog) {
    return originUrlGetter.apply(watchDog);
  }

  public abstract void process(
      WatchDogEntity watchDog,
      WatchDogBuilder watchDogBuilder,
      MembershipEnum membership,
      boolean isNewRun,
      Profile activeProfile);

  public abstract void handleError(WatchDogReportBuilder report, String errorMessage);
}
