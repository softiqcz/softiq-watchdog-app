package cz.softiq.service.watchdog.pages.search;

import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static cz.softiq.service.watchdog.model.enums.RealityEnums.SREALITY;
import static cz.softiq.service.watchdog.model.enums.SettingsEnums.FrequencyEnum.WEEKLY;
import static cz.softiq.service.watchdog.pages.search.SrealityConsentPage.acceptCookies;
import static cz.softiq.service.watchdog.pages.search.SrealityConsentPage.closePremiumBanner;
import static java.lang.Boolean.TRUE;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.ElementNotFound;
import com.codeborne.selenide.ex.ElementShould;
import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.core.email.CoreLogEntity;
import cz.softiq.core.service.ICoreSoftiqService;
import cz.softiq.service.automation.scraper.service.IScraperService;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogAutomationService;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogOfferService;
import cz.softiq.service.automation.watchdogSearch.service.SrealitySearchAddress;
import cz.softiq.service.automation.watchdogSearch.service.WatchDogOfferUtils;
import cz.softiq.service.watchdog.entity.WatchDogBuilder;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;

@Slf4j
public class SrealityPage
    implements IWatchDogAutomationService,
        IWatchDogOfferService,
        IScraperService,
        ICoreSoftiqService {

  private final SelenideElement exactAdressInput =
      $x("//input[@placeholder='Zadejte adresu, ulici, obec nebo stát']");

  private static void setTypeOfLabelText(String labelText, String[] types) {
    for (String type : types) {
      if (!StringUtils.isEmpty(type)) {
        SelenideElement element =
            $x(String.format("//div[text()='%s']/..//child::input[@name='%s']", labelText, type));
        executeJavaScript(
            "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
        element.click();
      }
    }
  }

  public WatchDogBuilder doSrealitySearch(
      WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    if (StringUtils.isEmpty(watchDog.getUrlSreality()) || watchDog.getDoResetSearch()) {
      open("https://www.sreality.cz/hledani/filtr");
      acceptCookies();
      $x("//h1[text()='Vyhledat nemovitost']").should(Condition.visible, Duration.ofSeconds(10));
      String estateType = watchDog.getEstateType();
      closePremiumBanner();
      selectOptions(estateType, "Typ nemovitosti");
      selectOptions(watchDog.getOfferType(), "Typ nabídky");
      if (watchDog.getEstateType().equals("Domy")) {
        // convert to pokoje
        selectOptions(convertFlatToHomeCategory(watchDog.getFlatCategory()), "Dispozice");
      } else {
        selectOptions(watchDog.getFlatCategory(), "Dispozice");
      }
      selectOptions(watchDog.getHomeCategory(), "Dispozice");
      selectOptions(watchDog.getLandCategory(), "Dispozice");
      selectOptions(watchDog.getComercialCategory(), "Dispozice");
      selectOptions(watchDog.getOtherCategory(), "Dispozice");
      $x("//div[@data-e2e='locality-section']/.//div[text()='Lokalita']").scrollTo();

      setAddress(watchDog.getExactAddress(), watchDog.getInRange());

      sleep(5000);
      $x("//fieldset/legend[text()='V okolí nemovitosti']").scrollTo();
      setPrice(watchDog.getPriceFrom(), watchDog.getPriceTo());

      if (estateType != null && !estateType.equals("Pozemky")) {
        $x("//fieldset/legend[text()='Stav']").scrollTo();
        setHomeDetails(watchDog);
      }

      $x("//button[@type='submit']").click();
      $x("//button[@id='estates-sorter-button']").should(Condition.visible, Duration.ofSeconds(30));
      SrealitySearchAddress.validate($x("//h1[@data-e2e='search-title']").getText(), watchDog);
      watchDogBuilder.setIsChangedUrl(true);
      saveUrl(watchDog, TRUE, SREALITY, watchDogBuilder);
    } else {
      open(watchDog.getUrlSreality());
      acceptCookies();
      $x("//button[@id='estates-sorter-button']").should(Condition.visible, Duration.ofSeconds(30));
    }
    return watchDogBuilder;
  }

  private String convertFlatToHomeCategory(String flatCategory) {
    if (flatCategory == null) {
      return null;
    }

    flatCategory = flatCategory.trim();

    if (flatCategory.equalsIgnoreCase("Atypický")) {
      return "Atypický";
    }

    if (flatCategory.equalsIgnoreCase("Pokoj")) {
      return "1 pokoj";
    }

    Matcher matcher =
        Pattern.compile("^(\\d+)\\s*\\+\\s*(kk|\\d+)$", Pattern.CASE_INSENSITIVE)
            .matcher(flatCategory);

    if (!matcher.matches()) {
      return null;
    }

    int rooms = Integer.parseInt(matcher.group(1));

    // +1 (or +2, etc.) is an additional separate room, +kk is not.
    if (!"kk".equalsIgnoreCase(matcher.group(2))) {
      rooms++;
    }

    switch (rooms) {
      case 1:
        return "1 pokoj";
      case 2:
      case 3:
      case 4:
        return rooms + " pokoje";
      default:
        return "5 a více pokojů";
    }
  }

  private void setAddress(String exactAddress, String inRange) {
    String address = StringUtils.normalizeSpace(exactAddress);
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException("Sreality locality must not be blank");
    }

    Duration timeout = Duration.ofSeconds(30);
    exactAdressInput.shouldBe(Condition.visible, timeout).scrollIntoCenter().setValue(address);

    // MUI loads grouped suggestions asynchronously. Click the actual option,
    // rather than sending keys before the suggestions have arrived.
    SelenideElement suggestions = autocompleteOptions(exactAdressInput);
    suggestions
        .$$("[role='option']")
        .findBy(Condition.matchText("(?is)^\\s*" + Pattern.quote(address) + "(?:\\s|,|$).*"))
        .shouldBe(Condition.visible, timeout)
        .click();

    SelenideElement localityChip =
        $("[data-e2e='multi-locality-chips']")
            .$$("[data-e2e='multi-locality-chip']")
            .findBy(Condition.text(address))
            .shouldBe(Condition.visible, timeout);
    exactAdressInput.shouldHave(Condition.exactValue(""), timeout);

    if (StringUtils.isNotBlank(inRange) && !"Nezadáno".equalsIgnoreCase(inRange.trim())) {
      setInRange(localityChip, inRange);
    }
  }

  private SelenideElement autocompleteOptions(SelenideElement input) {
    Duration timeout = Duration.ofSeconds(30);
    input
        .shouldHave(Condition.attribute("aria-expanded", "true"), timeout)
        .shouldHave(Condition.attributeMatching("aria-controls", ".+"), timeout);
    return $(By.id(input.getAttribute("aria-controls"))).shouldBe(Condition.visible, timeout);
  }

  private void setPrice(String priceFromValue, String priceToValue) {
    SelenideElement priceFrom = $x("//input[@name='price_from']");
    SelenideElement priceTo = $x("//input[@name='price_to']");
    priceFrom.setValue(priceFromValue);
    priceTo.setValue(priceToValue);
  }

  private void setInput(String fieldLabel, String value) {
    if (value != null) {
      SelenideElement inputElement = $x(String.format("//input[@placeholder='%s']", fieldLabel));
      executeJavaScript(
          "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", inputElement);
      inputElement.setValue(value);
    }
  }

  private void selectOptions(String typeList, String labelText) {
    if (!StringUtils.isBlank(typeList)) {
      String[] types = typeList.split(";\\s*");
      try {
        scrollToElement(labelText); // Scroll before selecting
        setTypeOfLabelText(labelText, types);
      } catch (ElementNotFound | ElementShould | ElementClickInterceptedException e) {
        String error = "Podmínka nenalezena " + typeList + " " + labelText + " " + e.getMessage();
        saveLog(new CoreLogEntity(null, LocalDate.now(), LocalDateTime.now(), error));
        log.error(error);
        throw new SoftiqException("Podmínka nenalezena");
      } catch (Throwable e) {
        String error =
            "Podmínka nemohla být zadána, zkouším znovu "
                + typeList
                + " "
                + labelText
                + " "
                + e.getMessage();
        saveLog(new CoreLogEntity(null, LocalDate.now(), LocalDateTime.now(), error));
        log.error(error);
        scrollToElement(labelText);
        setTypeOfLabelText(labelText, types);
      }
    }
  }

  private void scrollToElement(String labelText) {
    try {
      SelenideElement element = $x(String.format("//*[text()='%s']", labelText));
      element.scrollTo();
      executeJavaScript(
          "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
      element.shouldBe(Condition.visible);
    } catch (ElementShould e) {
      log.warn("First scroll failed for: " + labelText + ". Trying alternative scroll.");
      try {
        executeJavaScript(
            "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});",
            $x(String.format("//*[text()='%s']", labelText)).toWebElement());
        $x(String.format("//*[text()='%s']", labelText)).shouldBe(Condition.visible);
      } catch (Exception ex) {
        String error = String.format("Alternative scroll failed for: %s, %s", labelText, ex);
        saveLog(new CoreLogEntity(null, LocalDate.now(), LocalDateTime.now(), error));
        log.error(error);
      }
    }
  }

  private void setHomeDetails(WatchDogEntity watchDog) {
    if (watchDog.getEstateType().equals("Domy") || watchDog.getEstateType().equals("Byty")) {
      selectOptions(watchDog.getEstateCondition(), "Vlastnosti nemovitosti");
    }
    if (watchDog.getEstateType().equals("Byty")) {
      setInput("Podlaží od", watchDog.getFloorFrom());
      setInput("Podlaží do", watchDog.getFloorTo());
    }
    setInput("Užitná plocha od", watchDog.getUsableAreaFrom());
    setInput("Užitná plocha do", watchDog.getUsableAreaTo());
  }

  private void setFieldText(String fieldLabel, String value) {
    if (value != null) {
      sleep(3000);
      SelenideElement field =
          $x(String.format("//fieldset/legend[text()='%s']/..//div[@role='button']", fieldLabel));
      executeJavaScript(
          "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", field);
      if (field.isDisplayed()) {
        sleep(3000);
        field.click();
      } else {
        String error = "Unable to click on Stát";
        CoreLogEntity logEntity =
            new CoreLogEntity(null, LocalDate.now(), LocalDateTime.now(), error);
        saveLog(logEntity);
        log.error(error);
      }
      $x(String.format("//li[text()='%s']", value)).click();
    }
  }

  private void setInRange(SelenideElement localityChip, String value) {
    String radius = StringUtils.normalizeSpace(value).replaceFirst("^\\+\\s*", "");
    Duration timeout = Duration.ofSeconds(30);
    SelenideElement radiusInput =
        localityChip
            .$("input[role='combobox'][aria-label='Okruh']")
            .shouldBe(Condition.visible, timeout);
    radiusInput.click();
    autocompleteOptions(radiusInput)
        .$$("[role='option']")
        .findBy(Condition.exactText(radius))
        .shouldBe(Condition.visible, timeout)
        .click();
    radiusInput.shouldHave(Condition.value("+" + radius), timeout);
  }

  public List<WatchDogEstatesEntity> getLatestOffers(
      Set<String> existingOfferIds, WatchDogEntity watchDog) {
    SrealitySearchAddress.validate($x("//h1[@data-e2e='search-title']").getText(), watchDog);
    List<WatchDogEstatesEntity> newOffers = new ArrayList<>();
    ElementsCollection offers = $$x("//ul[@data-e2e]/li");
    for (int i = 0; i < offers.size(); i++) {
      SelenideElement offer = offers.get(i).shouldBe(Condition.visible);
      String offerId = WatchDogOfferUtils.normalizeSrealityOfferId(offer.getAttribute("id"));
      WatchDogEstatesEntity newOffer = null;
      if (!existingOfferIds.contains(offerId)
          && !StringUtils.isEmpty(offerId)
          && !offerId.contains("list-native")
          && !offerId.contains("broker-tip")) {
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
    String origin = "SREALITY.CZ";
    String url = offer.$x(".//a").getAttribute("href");
    String header = offer.$$x(".//p").get(0).getText();
    if (header.startsWith("TIP:")) {
      return null;
    }
    String address = offer.$$x(".//p").size() > 1 ? offer.$$x(".//p").get(1).getText() : "";
    String price =
        offer.$$x(".//p").size() > 2
            ? extractPrimaryPriceNumber(offer.$$x(".//p").get(2).getText())
            : "";

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
    acceptCookies();
    ((JavascriptExecutor) getWebDriver()).executeScript("window.open()");
    ArrayList<String> tabs = new ArrayList<>(getWebDriver().getWindowHandles());
    getWebDriver().switchTo().window(tabs.get(tabs.size() - 1));
    open(newOffer.getOfferUrl());
    String detailPrice =
        extractSrealityDetailPrice(getWebDriver().getPageSource(), newOffer.getOfferId());
    if (StringUtils.isNotBlank(detailPrice)) {
      newOffer.setPrice(detailPrice);
    }
    String insertedDate =
        $x("//dt[text()='Vloženo']/following-sibling::dd").scrollIntoCenter().getText();
    LocalDate today = LocalDate.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d. M. uuuu");
    String formattedDate = today.format(formatter);
    String imgUrl = $x("//div[@data-e2e='detail-gallery-desktop']//img[2]").getAttribute("src");
    newOffer.setMainPicUrl(imgUrl);
    newOffer.setIsUpdated(Boolean.valueOf(!insertedDate.equals(formattedDate)));
    String author =
        $x("//button[text()='Zobrazit e-mail']/../ul/li/a[contains(@href,'mailto:')]")
            .getAttribute("href")
            .replace("mailto:", "");
    contactIfNew(author, newOffer.getOrigin());
    getWebDriver().close();
    getWebDriver().switchTo().window(tabs.getFirst());
    return newOffer;
  }
}
