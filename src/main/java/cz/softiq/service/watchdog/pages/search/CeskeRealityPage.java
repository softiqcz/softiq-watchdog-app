package cz.softiq.service.watchdog.pages.search;

import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static cz.softiq.service.watchdog.model.enums.RealityEnums.CESKEREALITY;
import static cz.softiq.service.watchdog.model.enums.SettingsEnums.FrequencyEnum.WEEKLY;
import static java.lang.Boolean.TRUE;

import com.codeborne.selenide.*;
import com.codeborne.selenide.ex.ElementNotFound;
import com.codeborne.selenide.ex.ElementShould;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogAutomationService;
import cz.softiq.service.watchdog.entity.WatchDogBuilder;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.JavascriptExecutor;

@Slf4j
public class CeskeRealityPage implements IWatchDogAutomationService {

  public WatchDogBuilder doCeskeRealitySearch(
      WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    if (StringUtils.isEmpty(watchDog.getUrlCeskereality())
        || watchDog.getDoResetSearch()
        || watchDog
            .getUrlCeskereality()
            .equals("https://www.ceskereality.cz/real-estate/search/")) {
      open("https://www.ceskereality.cz");
      acceptCookies();
      $x("//h2[starts-with(text(),'Najděte si')]").should(Condition.exist, Duration.ofSeconds(10));
      $x("//h2/a[text()='" + watchDog.getEstateType() + "']").click();
      $x("//div[text()='Dispozice a stáří']").should(Condition.appear, Duration.ofSeconds(10));
      $x("//input[@name='filter[operace]' and @data-title='" + watchDog.getOfferType() + "']/..")
          .click();
      $x("//span[text()='Rozšířené hledání']").scrollIntoCenter().click();
      // address
      $x("//input[@id='filter_search']").scrollIntoCenter().setValue(watchDog.getSearchAddress());
      $x("//ul[@id='autoComplete_list_1']").should(Condition.visible, Duration.ofSeconds(10));
      $x("//ul[@id='autoComplete_list_1']").$x(".//li").click();
      $x("//select[@id='filter_vzdalenost']/..").click();
      try {
        $x("//div[@id='choices--filter_vzdalenost-item-choice-3']")
            .should(Condition.visible, Duration.ofSeconds(10));
        var inRange = watchDog.getInRange().replace(",", ".");
        if (inRange.contains("km")) {
          $x("//div[starts-with(@id,'choices--filter_vzdalenost-item-choice') and text()='"
                  + inRange
                  + "']")
              .click();
        }
      } catch (ElementShould e) {
        log.info("Range is disabled");
      }

      multiChoice("Dispozice", watchDog.getFlatCategory());
      multiChoice("Stav nemovitosti", watchDog.getEstateCondition());
      if (watchDog.getEstateType().equals("Komerční")) {
        multiChoice("Dispozice", watchDog.getComercialCategory());
      }
      SelenideElement searchButton = $x("//button[@id='filter_send']").scrollIntoCenter();
      searchButton.should(
          Condition.or(
              "search is ready",
              Condition.clickable,
              Condition.and(
                  "search has no offers", Condition.disabled, Condition.matchText("^Žádn.*"))),
          Duration.ofSeconds(30));
      if (searchButton.is(Condition.disabled)) {
        log.info("No Ceskereality offers match the watchdog filters; skipping search");
        return watchDogBuilder;
      }
      searchButton.click();
      try {
        $x("//p[contains(text(),'Právě teď můžete vybírat z')]")
            .should(Condition.visible, Duration.ofSeconds(30));
        watchDogBuilder.setHasOffers(true);
        watchDogBuilder.setIsChangedUrl(true);
        saveUrl(watchDog, TRUE, CESKEREALITY, watchDogBuilder);
      } catch (ElementShould | ElementNotFound e) {
        log.error("429 too many requests");
        watchDogBuilder.setIsSettingsSupported(false);
      }

    } else {
      open(watchDog.getUrlCeskereality());
      acceptCookies();
      $x("//p[contains(text(),'Právě teď můžete vybírat z')]")
          .should(Condition.visible, Duration.ofSeconds(30));
      watchDogBuilder.setHasOffers(true);
    }
    return watchDogBuilder;
  }

  private void acceptCookies() {
    try {
      SelenideElement consentFrame =
          $("iframe[title='Consent window']").should(Condition.exist, Duration.ofSeconds(10));
      getWebDriver().switchTo().frame(consentFrame);
      $x("//button[normalize-space()='Accept all']")
          .should(Condition.visible, Duration.ofSeconds(10))
          .click();
      log.info("Accepted cookies on ceskereality");
    } catch (ElementShould | ElementNotFound error) {
      log.info("No cookies on ceskereality");
    } finally {
      getWebDriver().switchTo().defaultContent();
    }
  }

  private void multiChoice(String type, String values) {
    boolean didClick5AndMore = false;
    if (values == null) {
      return;
    }
    for (String value : values.split(";")) {
      value = value.trim();
      if (value.equals("5+1 a více")) {
        didClick5AndMore = true;
      }
      if (value.equals("6 a více") && !didClick5AndMore) {
        value = "5+1 a více";
      }
      if (value.equals("Velmi dobrý")) {
        value = "Bezvadný";
      }
      if (value.equals("Ve výstavbě")) {
        value = "Rozestavěný";
      }
      if (value.equals("Před rekonstrukcí")) {
        value = "K rekonstrukci";
      }
      $x("//legend[text()='%s']/..//input[contains(@data-title,'%s')]/..".formatted(type, value))
          .scrollIntoCenter()
          .click();
    }
  }

  public List<WatchDogEstatesEntity> getLatestOffers(
      Set<String> existingOfferIds, WatchDogEntity watchDog) {
    List<WatchDogEstatesEntity> newOffers = new ArrayList<>();
    ElementsCollection offers = $$x("//article[@data-uid]");
    for (SelenideElement offer : offers) {
      String offerId = offer.getAttribute("data-uid");
      WatchDogEstatesEntity newOffer = null;
      if (!existingOfferIds.contains(offerId) && !StringUtils.isEmpty(offerId)) {
        newOffer = getOfferDetails(offer, watchDog, offerId);
      }
      if (newOffer != null) {
        newOffers.add(newOffer);
      }
    }

    return newOffers;
  }

  private WatchDogEstatesEntity getOfferDetails(
      SelenideElement offer, WatchDogEntity watchDog, String offerId) {
    String origin = "CESKEREALITY.CZ";
    var offerHeadline = offer.$x(".//h2");
    String url = offerHeadline.$x(".//a").getAttribute("href");
    String headerAndAddress = offerHeadline.getText();
    String header = headerAndAddress.split("m²")[0];
    String address = headerAndAddress.split("m² ")[1];
    String price =
        offer.$(".i-estate__footer-price-value, h3.price").getText().replaceAll("[^0-9]", "");

    var newOffer =
        new WatchDogEstatesEntity(
            null,
            watchDog,
            origin,
            offerId,
            header,
            address,
            price,
            null,
            null,
            Boolean.FALSE,
            TRUE,
            Boolean.FALSE,
            Boolean.FALSE,
            Boolean.FALSE,
            url,
            null,
            LocalDate.now(),
            null,
            null);

    // verify if new or just updated
    // for first sync, always skip notification
    boolean notifyClient = watchDog.getIsFirstSyncCompleted();
    newOffer = openNewTabAndVerify(newOffer);

    Boolean isUpdated = newOffer.getIsUpdated();
    Boolean doHideForEver = Boolean.FALSE;
    Boolean isFirstSyncCompleted = Boolean.valueOf(notifyClient);

    switch (watchDog.getSendOfferAs()) {
      case UPDATED:
        notifyClient = isUpdated;
        doHideForEver = Boolean.valueOf(!isUpdated);
        break;
      case NEW:
        notifyClient = !isUpdated;
        doHideForEver = isUpdated;
        break;
      case ALL:
        notifyClient = TRUE;
        doHideForEver = Boolean.FALSE;
        break;
    }

    newOffer.setIsUpdated(isUpdated);

    newOffer.setIsReported(
        Boolean.valueOf(
            watchDog.getEmailReportFrequency().equals(WEEKLY)
                ? TRUE
                : !isFirstSyncCompleted || !notifyClient));
    newOffer.setIsHiddenFromClientByAdmin(
        watchDog.getEmailReportFrequency().equals(WEEKLY) ? TRUE : Boolean.valueOf(notifyClient));
    newOffer.setIsHiddenFromClientByUser(Boolean.valueOf(doHideForEver));

    return newOffer;
  }

  private WatchDogEstatesEntity openNewTabAndVerify(WatchDogEstatesEntity newOffer) {
    ((JavascriptExecutor) getWebDriver()).executeScript("window.open()");
    ArrayList<String> tabs = new ArrayList<>(getWebDriver().getWindowHandles());
    getWebDriver().switchTo().window(tabs.get(tabs.size() - 1));
    open(newOffer.getOfferUrl());
    boolean insertedToday =
        $x("//div[span[text()='Datum vložení']]/span[2]").getText().equals("Dnes");
    newOffer.setIsUpdated(!insertedToday);

    String imgUrl = $x("//a[@data-caption]").getAttribute("href");
    newOffer.setMainPicUrl(imgUrl);
    getWebDriver().close();
    getWebDriver().switchTo().window(tabs.getFirst());
    return newOffer;
  }
}
