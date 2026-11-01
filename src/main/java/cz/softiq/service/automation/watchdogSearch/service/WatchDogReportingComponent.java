package cz.softiq.service.automation.watchdogSearch.service;

import static cz.softiq.core.configuration.profile.Profile.DEV;

import cz.softiq.core.configuration.profile.Profile;
import cz.softiq.service.softiq.service.email.IEmailService;
import cz.softiq.service.watchdog.component.WatchDogManagementComponent;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import cz.softiq.service.watchdog.model.enums.SettingsEnums;
import cz.softiq.service.watchdog.repository.WatchDogEstatesRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class WatchDogReportingComponent implements IEmailService {
  private final WatchDogManagementComponent watchDogManagementComponent;
  private final WatchDogEstatesRepository watchDogEstatesRepository;

  public void reportWatchDogs(Boolean isDailyRun, Boolean isNonstopRun, Profile activeProfile) {
    List<WatchDogEntity> watchDogs = getWatchDogsForReport(isDailyRun, isNonstopRun);
    List<WatchDogEntity> watchDogsGroupedByClient = new ArrayList<>();

    for (WatchDogEntity watchDog : watchDogs) {
      processWatchDog(watchDog, watchDogsGroupedByClient, activeProfile);
    }

    reportOffersGroupedByClient(watchDogsGroupedByClient, activeProfile);
  }

  private List<WatchDogEntity> getWatchDogsForReport(Boolean isDailyRun, Boolean isNonstopRun) {
    List<WatchDogEntity> watchDogs = new ArrayList<>();
    if (isDailyRun) {
      watchDogs.addAll(
          watchDogManagementComponent.getAllWatchDogsWithFrequency(
              SettingsEnums.FrequencyEnum.DAILY));
    }
    if (isNonstopRun) {
      watchDogs.addAll(
          watchDogManagementComponent.getAllWatchDogsWithFrequency(
              SettingsEnums.FrequencyEnum.NONSTOP));
    } else {
      watchDogs.addAll(
          watchDogManagementComponent.getAllWatchDogsWithFrequency(
              SettingsEnums.FrequencyEnum.OFTEN));
    }
    return watchDogs;
  }

  private void processWatchDog(
      WatchDogEntity watchDog,
      List<WatchDogEntity> watchDogsGroupedByClient,
      Profile activeProfile) {
    if (!Boolean.TRUE.equals(watchDog.getDoEmailReport())) {
      markOffersAsReported(watchDogManagementComponent.getNewWatchDogEstateEntities(watchDog));
      return;
    }

    switch (watchDog.getEmailReportSetting()) {
      case GROUP_BY_ALL -> watchDogsGroupedByClient.add(watchDog);
      case GROUP_BY_WATCHDOG -> reportGroupedOffers(
          watchDog.getClient().getEmail(),
          watchDogManagementComponent.getNewWatchDogEstateEntities(watchDog),
          activeProfile);
      case SINGLE -> reportSingleOffers(
          watchDog, watchDogManagementComponent.getNewWatchDogEstateEntities(watchDog));
    }
  }

  private void reportOffersGroupedByClient(List<WatchDogEntity> watchDogs, Profile activeProfile) {
    watchDogs.stream()
        .collect(Collectors.groupingBy(WatchDogEntity::getClient))
        .forEach(
            (client, clientWatchDogs) -> {
              List<WatchDogEstatesEntity> offers =
                  clientWatchDogs.stream()
                      .flatMap(
                          watchDog ->
                              watchDogManagementComponent
                                  .getNewWatchDogEstateEntities(watchDog)
                                  .stream())
                      .toList();
              reportGroupedOffers(client.getEmail(), offers, activeProfile);
            });
  }

  private void reportSingleOffers(WatchDogEntity watchDog, List<WatchDogEstatesEntity> newOffers) {
    for (WatchDogEstatesEntity newOffer : newOffers) {
      watchDogReportSingle(newOffer, watchDog.getClient().getEmail());
      markOfferAsReported(newOffer);
    }
  }

  private void reportGroupedOffers(
      String clientEmail, List<WatchDogEstatesEntity> newOffers, Profile activeProfile) {
    if (newOffers.isEmpty()) {
      return;
    }

    List<List<WatchDogEstatesEntity>> groupedOffers =
        new ArrayList<>(
            newOffers.stream()
                .filter(offer -> Boolean.TRUE.equals(offer.getWatchdogEntity().getDoEmailReport()))
                .collect(Collectors.groupingBy(offer -> offer.getWatchdogEntity().getId()))
                .values());
    if (!groupedOffers.isEmpty()) {
      watchDogReport(groupedOffers, DEV.equals(activeProfile) ? "svejdam@icloud.com" : clientEmail);
    }
    markOffersAsReported(newOffers);
  }

  private void markOffersAsReported(List<WatchDogEstatesEntity> newOffers) {
    newOffers.forEach(this::markOfferAsReported);
  }

  private void markOfferAsReported(WatchDogEstatesEntity newOffer) {
    newOffer.setIsReported(true);
    newOffer.setIsHiddenFromClientByAdmin(false);
    watchDogEstatesRepository.save(newOffer);
  }
}
