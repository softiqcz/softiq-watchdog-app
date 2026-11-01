package cz.softiq.service.automation.watchdogSearch.service;

import static cz.softiq.core.configuration.profile.Profile.DEV;
import static cz.softiq.service.automation.watchdogSearch.service.WatchDogOfferUtils.*;

import cz.softiq.core.configuration.profile.Profile;
import cz.softiq.service.watchdog.entity.WatchDogBuilder;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import cz.softiq.service.watchdog.model.builder.WatchDogReportBuilder;
import cz.softiq.service.watchdog.model.enums.MembershipEnum;
import cz.softiq.service.watchdog.model.enums.RealityProvider;
import cz.softiq.service.watchdog.repository.WatchDogEstatesRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchComponent implements IWatchDogAutomationService {
  private final WatchDogEstatesRepository watchDogEstatesRepository;
  private final WatchdogResultAnalysisComponent watchdogResultAnalysisComponent;

  public Set<String> getExistingOfferIds(WatchDogEntity watchDog) {
    Set<String> existingOfferIds =
        watchDogEstatesRepository.findAllWatchDogEstateEntities(watchDog).stream()
            .map(WatchDogEstatesEntity::getOfferId)
            .filter(Objects::nonNull)
            .flatMap(offerId -> Stream.of(offerId, normalizeSrealityOfferId(offerId)))
            .collect(Collectors.toSet());
    return existingOfferIds;
  }

  public void saveNewOffers(List<WatchDogEstatesEntity> newOffers) {
    if (getActiveProfile().equals(DEV)) {
      return;
    }

    if (newOffers != null && !newOffers.isEmpty()) {
      List<WatchDogEstatesEntity> validOffers =
          newOffers.stream().filter(Objects::nonNull).collect(Collectors.toList());
      int skippedOffers = newOffers.size() - validOffers.size();
      if (skippedOffers > 0) {
        log.warn("Skipping {} null watchdog offer(s) before persistence", skippedOffers);
      }
      if (validOffers.isEmpty()) {
        return;
      }

      validOffers.forEach(WatchDogOfferUtils::populateMissingAreaAndPricePerSquareMeter);

      validOffers.stream()
          .collect(
              Collectors.groupingBy(
                  offer ->
                      "watchdog="
                          + offer.getWatchdogEntity().getId()
                          + ", origin="
                          + offer.getOrigin(),
                  Collectors.counting()))
          .forEach((key, count) -> log.debug("Persisting {} offers for {}", count, key));

      Map<Integer, List<WatchDogEstatesEntity>> offersByWatchDog = new HashMap<>();

      for (WatchDogEstatesEntity newOffer : validOffers) {
        WatchDogEntity watchDog = newOffer.getWatchdogEntity();
        List<WatchDogEstatesEntity> offersToCompare =
            offersByWatchDog.computeIfAbsent(
                watchDog.getId(),
                ignored ->
                    new ArrayList<>(
                        watchDogEstatesRepository.findAllWatchDogEstateEntities(watchDog)));

        Optional<String> foreignReason =
            findForeignCountry(newOffer.getAddress()).map(country -> "country=" + country);
        if (foreignReason.isEmpty()) {
          foreignReason =
              findForeignHeaderKeyword(newOffer.getHeader())
                  .map(keyword -> "headerKeyword=" + keyword);
        }

        if (foreignReason.isPresent()) {
          markAsForeignOffer(newOffer, foreignReason.get());
        } else {
          offersToCompare.stream()
              .filter(existingOffer -> isDuplicate(newOffer, existingOffer))
              .findFirst()
              .ifPresent(existingOffer -> markAsDuplicate(newOffer, existingOffer));
        }
        offersToCompare.add(newOffer);
      }

      validOffers.stream()
          .collect(Collectors.groupingBy(offer -> offer.getWatchdogEntity().getId()))
          .values()
          .forEach(
              offers ->
                  watchdogResultAnalysisComponent.populateSoftiqIndexes(
                      offers.getFirst().getWatchdogEntity(), offers));

      watchDogEstatesRepository.saveAll(validOffers);
    }
  }

  private void markAsDuplicate(
      WatchDogEstatesEntity duplicateOffer, WatchDogEstatesEntity existingOffer) {
    duplicateOffer.setIsHiddenFromClientByAdmin(true);
    duplicateOffer.setIsReported(true);
  }

  private void markAsForeignOffer(WatchDogEstatesEntity offer, String reason) {
    offer.setIsHiddenFromClientByAdmin(true);
    offer.setIsReported(true);
    log.info(
        "Hiding foreign watchdog offer: watchdogId={}, origin={}, offerId={}, reason={}, address={}, header={}",
        offer.getWatchdogEntity().getId(),
        offer.getOrigin(),
        offer.getOfferId(),
        reason,
        offer.getAddress(),
        offer.getHeader());
  }

  public WatchDogReportBuilder doSearch(
      WatchDogEntity watchDog, MembershipEnum membership, Profile activeProfile) {
    WatchDogReportBuilder report = WatchDogReportBuilder.builder().build();
    boolean isNewRun = !watchDog.getIsFirstSyncCompleted() || watchDog.getDoResetSearch();
    boolean isSuccess = true;

    for (RealityProvider searchLogicProvider : RealityProvider.values()) {
      if (watchDog.getRealityUrl().contains(searchLogicProvider.getName())) {
        var watchDogBuilder = WatchDogBuilder.builder().watchDogEntity(watchDog).build();
        if (getActiveProfile().equals(DEV)) {
          searchLogicProvider.process(
              watchDog, watchDogBuilder, membership, isNewRun, activeProfile);
        } else {
          try {
            searchLogicProvider.process(
                watchDog, watchDogBuilder, membership, isNewRun, activeProfile);
          } catch (Throwable e) {
            isSuccess = false;
            log.error("{} search failed", searchLogicProvider.getName(), e);
            searchLogicProvider.handleError(
                report, getErrorMessage(e, searchLogicProvider.getOriginUrl(watchDog)));
          }
        }
      }
    }

    report.setIsSuccess(isSuccess);
    return report;
  }

  public String getErrorMessage(Throwable throwable, String originUrl) {
    Throwable rootCause = throwable;
    while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
      rootCause = rootCause.getCause();
    }

    String message = rootCause.getMessage();
    if (message == null || message.isBlank()) {
      message = throwable.getMessage();
    }
    if (message == null || message.isBlank()) {
      message = rootCause.getClass().getSimpleName() + " (no exception message provided)";
    } else {
      message = rootCause.getClass().getSimpleName() + ": " + message;
    }
    return message + " [URL: " + originUrl + "]";
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
