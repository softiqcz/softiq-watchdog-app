package cz.softiq.service.automation.csssro.web;

import static com.codeborne.selenide.Selenide.*;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cz.softiq.common.base.BaseBrowserClass;
import cz.softiq.core.email.CoreLogEntity;
import cz.softiq.service.automation.offerSync.service.rest.ActiveOfferMatcher;
import cz.softiq.service.automation.offerSync.service.rest.IRestApiService;
import cz.softiq.service.watchdog.pages.search.SrealityConsentPage;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SrealityWeb extends BaseBrowserClass implements IRestApiService {

  public SrealityWeb openWeb(String url) {
    open(url);
    acceptCookies();
    $x("//h2[text()='O makléři']").should(Condition.visible, Duration.ofSeconds(30));
    return this;
  }

  public void acceptCookies() {
    SrealityConsentPage.acceptCookies();
  }

  public List<Integer> syncAllOffers(
      String token, String username, String url, List<JsonNode> activeOffers) {
    List<Integer> onWeb = new ArrayList<>();
    int page = 1;
    while (true) {
      ElementsCollection offers = $$x("//ul[@data-e2e='estates-list']/li/a");
      if (offers.isEmpty()) break;

      log.info("Syncing page {} of offers", page);
      for (SelenideElement offer : offers) {
        syncOffer(offer, onWeb, token, username, url, activeOffers);
      }

      SelenideElement nextButton = $x("//a[@class='paging-next']");
      if (nextButton.exists() && nextButton.isDisplayed()) {
        nextButton.click();
        sleep(2000);
      } else {
        break;
      }
      page++;
      if (page > 20) break; // Safety break
    }
    return onWeb;
  }

  public void syncOffer(
      SelenideElement offer,
      List<Integer> onWeb,
      String token,
      String username,
      String url,
      List<JsonNode> activeOffers) {
    String document = new EstatePage().getOfferDetails(offer, false, token, username);
    // upload to api
    log.info("Updating offer: {}", offer.getText());
    int id = 0;
    try {
      ObjectNode request = (ObjectNode) new ObjectMapper().readTree(document);
      ObjectNode requestData = (ObjectNode) request.path("data");
      ActiveOfferMatcher.addMatchingActiveOfferId(requestData, activeOffers)
          .ifPresent(existingId -> log.info("Updating existing active offer: {}", existingId));
      document = new ObjectMapper().writeValueAsString(request);
      id = postJson(document, token, url);
    } catch (Exception e) {
      String error = "Failed to sync offer: " + offer.getText() + " " + e.getMessage();

      log.error(document);
      CoreLogEntity logEntity =
          new CoreLogEntity(null, LocalDate.now(), LocalDateTime.now(), error);
      saveLog(logEntity);
      log.error(error);
    }
    onWeb.add(id);
  }
}
