package cz.softiq.service.automation.watchdogSearch.service;

import com.fasterxml.jackson.databind.JsonNode;
import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.core.configuration.profile.Profile;
import cz.softiq.core.email.CoreLogEntity;
import cz.softiq.core.helper.ReportBuilder;
import cz.softiq.core.service.ICoreSoftiqService;
import cz.softiq.service.automation.csssro.service.sync.IOfferSyncService;
import cz.softiq.service.automation.csssro.web.SrealityWeb;
import cz.softiq.service.automation.offerSync.service.rest.IRestApiService;
import cz.softiq.service.watchdog.entity.WatchDogBuilder;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import cz.softiq.service.watchdog.pages.search.SrealityPage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
class SrealityComponent
    implements IBaseService, IRestApiService, IOfferSyncService, IBeanProvider, ICoreSoftiqService {
  private final SearchComponent searchComponent;
  private final WatchDogHtmlOfferParser htmlOfferParser;
  private final WatchDogSyncMode syncMode;

  @Value("${spring.mail.username}")
  private String username;

  @Value("${secret.automation.password}")
  private String password;

  private String token;

  private final String SREALITY_CSS_URL =
      "https://www.sreality.cz/adresar/ceska-stavebne-spravni-ostrava-poruba/29663/makleri/95265";
  private final String SREALITY_LONGPRO_URL =
      "https://www.sreality.cz/adresar/marcel-dlouhy-realitni-makler-longpro-s-r-o-praha-cerny-most/11639/makleri/299396";

  public WatchDogBuilder doSrealitySearch(
      WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    if (syncMode.isHtmlMode()
        && !StringUtils.isEmpty(watchDog.getUrlSreality())
        && !watchDog.getDoResetSearch()) {
      return watchDogBuilder;
    }
    return new SrealityPage().doSrealitySearch(watchDog, watchDogBuilder);
  }

  public void getLatestOffers(WatchDogEntity watchDog, Profile activeProfile) {
    if (activeProfile != Profile.DEV) {
      Set<String> existingOfferIds = searchComponent.getExistingOfferIds(watchDog);
      var newOffers = getLatestSrealityOffers(watchDog, existingOfferIds);
      searchComponent.saveNewOffers(newOffers);
    } else {
      Set<String> existingOfferIds = new HashSet<>();
      getLatestSrealityOffers(watchDog, existingOfferIds);
    }
  }

  private List<WatchDogEstatesEntity> getLatestSrealityOffers(
      WatchDogEntity watchDog, Set<String> existingOfferIds) {
    SrealityPage page = new SrealityPage();
    if (syncMode.isHtmlMode() && StringUtils.isNotEmpty(watchDog.getUrlSreality())) {
      return htmlOfferParser.getSrealityOffers(watchDog, existingOfferIds);
    }
    return page.getLatestOffers(existingOfferIds, watchDog);
  }

  public ReportBuilder syncPronajemSnadno() {
    String url = "https://pronajemsnadno.cz";

    token = getToken(url);
    ReportBuilder reportMessage = ReportBuilder.builder().build();
    return pronajemSnadnoFlowActive(url, reportMessage);
  }

  public ReportBuilder syncCssSro() {
    String url = "https://css-sro.cz";

    token = getToken(url);
    ReportBuilder reportMessage = ReportBuilder.builder().build();

    return cssSroFlowActive(url, reportMessage);
  }

  private ReportBuilder cssSroFlowActive(String url, ReportBuilder reportMessage) {

    new SrealityWeb().openWeb(SREALITY_CSS_URL);
    List<Integer> misssingOffers;
    List<Integer> newActiveOffers;

    List<JsonNode> activeOfferDetails = getActiveOfferDetailsApi(url);
    List<Integer> activeOffersInDb = activeOfferIds(activeOfferDetails);
    List<Integer> onWebActive = null;

    try {
      onWebActive = getOffers(token, username, url, activeOfferDetails);
    } catch (Throwable exception) {
      String error = "Aktivní obchody failed for client css-sro";
      CoreLogEntity logEntity =
          new CoreLogEntity(null, LocalDate.now(), LocalDateTime.now(), error);
      saveLog(logEntity);
      log.error(error);
      reportMessage.setErrorMessage(error);
    }

    if ((onWebActive == null || onWebActive.isEmpty()) && activeOffersInDb.size() > 1) {
      log.warn(
          "No active offers found on web, skipping 'mark as sold' logic to prevent data loss.");
      reportMessage.setNewActiveOffers(new ArrayList<>());
      reportMessage.setNewSoldOffers(new ArrayList<>());
      return reportMessage;
    }

    newActiveOffers = findMissingIds(onWebActive, activeOffersInDb);
    misssingOffers = findMissingIds(activeOffersInDb, onWebActive);
    reportMessage.setNewActiveOffers(newActiveOffers);
    log.info("These offers were active last time, but are not active now " + misssingOffers);
    List<Integer> soldOffersForClient = new ArrayList<>();
    for (Integer offerId : misssingOffers) {
      boolean markedAsSold = markAsSoldJson(offerId, token, url);
      if (markedAsSold) {
        soldOffersForClient.add(offerId);
      }
    }
    reportMessage.setNewSoldOffers(soldOffersForClient);
    return reportMessage;
  }

  private ReportBuilder pronajemSnadnoFlowActive(String url, ReportBuilder reportMessage) {
    new SrealityWeb().openWeb(SREALITY_LONGPRO_URL);
    List<Integer> misssingOffers;
    List<Integer> newActiveOffers;

    List<JsonNode> activeOfferDetails = getActiveOfferDetailsApi(url);
    List<Integer> activeOffersInDb = activeOfferIds(activeOfferDetails);
    List<Integer> onWebActive = null;

    try {
      onWebActive = getOffers(token, username, url, activeOfferDetails);
    } catch (Throwable exception) {
      String error = "Aktivní obchody failed for client longpro";
      CoreLogEntity logEntity =
          new CoreLogEntity(null, LocalDate.now(), LocalDateTime.now(), error);
      saveLog(logEntity);
      log.error(error);
      reportMessage.setErrorMessage(error);
    }

    if ((onWebActive == null || onWebActive.isEmpty()) && activeOffersInDb.size() > 1) {
      log.warn(
          "No active offers found on web, skipping 'mark as sold' logic to prevent data loss.");
      reportMessage.setNewActiveOffers(new ArrayList<>());
      reportMessage.setNewSoldOffers(new ArrayList<>());
      return reportMessage;
    }

    newActiveOffers = findMissingIds(onWebActive, activeOffersInDb);
    misssingOffers = findMissingIds(activeOffersInDb, onWebActive);
    reportMessage.setNewActiveOffers(newActiveOffers);
    log.info("These offers were active last time, but are not active now " + misssingOffers);
    List<Integer> soldOffersForClient = new ArrayList<>();
    for (Integer offerId : misssingOffers) {
      boolean markedAsSold = markAsSoldJson(offerId, token, url);
      if (markedAsSold) {
        soldOffersForClient.add(offerId);
      }
    }
    reportMessage.setNewSoldOffers(soldOffersForClient);
    return reportMessage;
  }

  private List<Integer> activeOfferIds(List<JsonNode> activeOffers) {
    return activeOffers.stream()
        .map(offer -> offer.path("id").asInt())
        .filter(id -> id > 0)
        .toList();
  }
}
