package cz.softiq.service.watchdog.pages.search;

import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static cz.softiq.service.watchdog.model.enums.SettingsEnums.FrequencyEnum.WEEKLY;
import static java.lang.Boolean.TRUE;

import com.codeborne.selenide.ClickOptions;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.ElementNotFound;
import com.codeborne.selenide.ex.ElementShould;
import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogAutomationService;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogOfferService;
import cz.softiq.service.watchdog.entity.WatchDogBuilder;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import cz.softiq.service.watchdog.model.enums.RealityEnums;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.testng.Assert;

@Slf4j
public class BezrealitkyPage implements IWatchDogAutomationService, IWatchDogOfferService {

  private static final Duration SEARCH_RESULTS_TIMEOUT = Duration.ofSeconds(30);
  private static final long ADDRESS_AUTOCOMPLETE_WAIT_MILLIS = 2_000;
  private static final long COOKIES_WAIT = 5_000;
  private static final long SEARCH_REFRESH_WAIT_MILLIS = 1_000;
  private static final int ADDRESS_REPAIR_ATTEMPTS = 3;
  private static final Set<Integer> PUBLIC_WATCHDOG_IDS = Set.of(666);

  public WatchDogBuilder doBezrealitkySearch(
      WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    String savedUrl = watchDog.getUrlBezrealitky();
    boolean isSavedUrlValid = isBezrealitkyUrl(savedUrl);
    if (StringUtils.isNotEmpty(savedUrl) && !isSavedUrlValid) {
      log.error(
          "Watchdog {} has an invalid Bezrealitky URL '{}'. Rebuilding the search instead of opening it.",
          watchDog.getId(),
          savedUrl);
    }

    if (StringUtils.isEmpty(savedUrl) || watchDog.getDoResetSearch() || !isSavedUrlValid) {
      open("https://www.bezrealitky.cz");
      $x("//span[text()='Typ nabídky']").should(Condition.visible, Duration.ofSeconds(10));
      acceptCookies();
      try {
        setOfferType(watchDog);
      } catch (Throwable e) {
        acceptCookies();
        setOfferType(watchDog);
      }
      setEstateType(watchDog);
      setAddress(watchDog);
      setPrice(watchDog);

      $x("//a[contains(@href,'/vyhledat')]/button").click();
      waitForSearchResults();
      var searchButton = $x("//button/span[text()='Vyhledat']");
      var searchButtonInDropdown = $x("//button/span[text()='Vyhledat nemovitosti']");
      ensureAddressIsSet(watchDog, watchDogBuilder, searchButton);

      String flatCategory = watchDog.getFlatCategory();
      if (!StringUtils.isBlank(flatCategory)) {
        setFlatCategory(flatCategory);
        searchButton.click();
      }

      String estateCondition = watchDog.getEstateCondition();

      if (!StringUtils.isBlank(estateCondition)
          && !"Pozemky".equals(watchDog.getEstateType())
          && !("Komerční".equals(watchDog.getEstateType())
              && "Kanceláře".equals(watchDog.getComercialCategory()))) {
        setEstateCondition(estateCondition);
        searchButtonInDropdown.click();
      }

      watchDogBuilder.setIsChangedUrl(true);
      saveUrl(watchDog, Boolean.TRUE, RealityEnums.BEZREALITKY, watchDogBuilder);
    } else {
      open(watchDog.getUrlBezrealitky());
      acceptCookies();
      ensureAddressIsSet(watchDog, watchDogBuilder, $x("//button/span[text()='Vyhledat']"));
    }
    // verify address is saved
    String searchAddress = watchDog.getSearchAddress();
    SelenideElement adressElement = $x("//input[@placeholder='Kraj, okres, město, adresa']");
    String searchedValue = adressElement.getValue();
    if (!PUBLIC_WATCHDOG_IDS.contains(watchDog.getId())) {
      Assert.assertTrue(
          normalizeToSet(searchedValue).containsAll(normalizeToSet(searchAddress)),
          "Did not enter correct address.");
    }
    return watchDogBuilder;
  }

  private boolean isBezrealitkyUrl(String url) {
    if (StringUtils.isBlank(url)) {
      return false;
    }
    try {
      String host = URI.create(url).getHost();
      return host != null
          && (host.equalsIgnoreCase("bezrealitky.cz")
              || host.toLowerCase(Locale.ROOT).endsWith(".bezrealitky.cz"));
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private void ensureAddressIsSet(
      WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder, SelenideElement searchButton) {
    String searchAddress = watchDog.getSearchAddress();
    SelenideElement addressElement =
        $x("//input[@placeholder='Kraj, okres, město, adresa']")
            .should(Condition.visible, SEARCH_RESULTS_TIMEOUT);

    if (addressMatches(addressElement.getValue(), searchAddress)) {
      return;
    }

    log.warn(
        "Bezrealitky opened without the expected address for watchdog {}. Expected '{}', got '{}', URL '{}'. Repairing the search.",
        watchDog.getId(),
        searchAddress,
        addressElement.getValue(),
        getWebDriver().getCurrentUrl());

    for (int attempt = 1; attempt <= ADDRESS_REPAIR_ATTEMPTS; attempt++) {
      enterAddress(addressElement, searchAddress);
      searchButton.click();
      waitForSearchResults();
      sleep(SEARCH_REFRESH_WAIT_MILLIS);

      String searchedValue = addressElement.getValue();
      if (addressMatches(searchedValue, searchAddress)) {
        watchDogBuilder.setIsChangedUrl(true);
        saveUrl(watchDog, Boolean.TRUE, RealityEnums.BEZREALITKY, watchDogBuilder);
        log.info(
            "Repaired Bezrealitky address for watchdog {} on attempt {}. URL '{}'.",
            watchDog.getId(),
            attempt,
            getWebDriver().getCurrentUrl());
        return;
      }
    }

    Assert.fail(
        String.format(
            "Did not enter correct address. Expected '%s', got '%s', URL '%s'.",
            searchAddress, addressElement.getValue(), getWebDriver().getCurrentUrl()));
  }

  private boolean addressMatches(String searchedValue, String searchAddress) {
    return StringUtils.isNotBlank(searchedValue)
        && normalizeToSet(searchedValue).containsAll(normalizeToSet(searchAddress));
  }

  private void waitForSearchResults() {
    $x("//select/option[@value = 'TIMEORDER_DESC']")
        .should(Condition.exist, SEARCH_RESULTS_TIMEOUT);
  }

  private Set<String> normalizeToSet(String input) {
    return Arrays.stream(input.toLowerCase().replaceAll("[,]", "").split("\\s+"))
        .collect(Collectors.toSet());
  }

  private void acceptCookies() {
    try {
      sleep(COOKIES_WAIT);
      var cookies = $x("//button[@id='CybotCookiebotDialogBodyLevelButtonLevelOptinAllowAll']");
      if (cookies.exists()) cookies.click();
    } catch (ElementNotFound | ElementClickInterceptedException | ElementShould e) {
      log.info("No cookies popup " + e.getMessage());
    }
  }

  private void setOfferType(WatchDogEntity watchDog) {
    String offerType = watchDog.getOfferType();
    if (offerType.contains("Pronájem") || offerType.contains("Prodej")) {
      $x("//button[contains(@class,'SearchPanel')]//span[text()='Typ nabídky']").click();
      if (offerType.contains("Pronájem")) {
        $x("//input[@name='property-offer-type' and @id='PRONAJEM']").setSelected(true);
      } else if (offerType.contains("Prodej")) {
        $x("//input[@name='property-offer-type' and @id='PRODEJ']").setSelected(true);
      }
    } else {
      throw new SoftiqException("Unsopported offer type " + offerType);
    }
  }

  private void setEstateType(WatchDogEntity watchDog) {
    String estateType = watchDog.getEstateType();
    $x("//button[contains(@class,'SearchPanel')]//span[text()='Nemovitost']").click();
    if (estateType.contains("Byty")) {
      $x("//input[@name='property-estate-type' and @id='BYT']").setSelected(true);
    } else if (estateType.contains("Domy")) {
      $x("//input[@name='property-estate-type' and @id='DUM']").setSelected(true);
    } else if (estateType.contains("Pozemky")) {
      $x("//input[@name='property-estate-type' and @id='POZEMEK']").setSelected(true);
    } else if (estateType.contains("Komerční")
        && watchDog.getComercialCategory().contains("Kanceláře")) {
      $x("//input[@name='property-estate-type' and @id='KANCELAR']").setSelected(true);
    } else {
      throw new SoftiqException("Unsopported estate type " + estateType);
    }
  }

  private void setEstateCondition(String estateCondition) {
    List<String> conditions = new ArrayList<>(List.of(estateCondition.split(";\\s*")));

    $x("//button/span[text()='Další parametry']").scrollTo().click(ClickOptions.usingJavaScript());
    $x("//button/span[text()='Stav budovy']").scrollTo().click(ClickOptions.usingJavaScript());

    var dropdown = $x("//div[@class='accordion-collapse collapse show']");
    dropdown.should(Condition.appear).scrollTo();

    for (String type : conditions) {
      if (StringUtils.isNotBlank(type)) {
        dropdown.$x(String.format(".//label/span[text()='%s']", type)).scrollTo().click();
      }
    }
  }

  private void setFlatCategory(String flatCategory) {
    List<String> types = new ArrayList<>(List.of(flatCategory.split(";\\s*")));

    if (types.contains("6 a více")) {
      types.remove("6 a více");
      types.add("6+kk");
      types.add("6+1");
      types.add("7+kk");
      types.add("7+1");
    }

    var flatCategoryInput = $x("//div[@aria-labelledby='Dispozice']//span");
    flatCategoryInput.click();

    var dropdown = $x("//div[@class='dropdown-content']");
    dropdown.should(Condition.visible, Duration.ofSeconds(10));

    for (String type : types) {
      if (StringUtils.isNotBlank(type)) {
        dropdown.$x(String.format(".//*[text()='%s']", type)).scrollTo();
        flatCategoryInput.scrollTo();
        dropdown.$x(String.format(".//*[text()='%s']", type)).click(ClickOptions.usingJavaScript());
      }
    }
  }

  private void setAddress(WatchDogEntity watchDog) {
    enterAddress(
        $x("//input[@placeholder='Kraj, okres nebo adresa…']")
            .should(Condition.visible, SEARCH_RESULTS_TIMEOUT),
        watchDog.getSearchAddress());
  }

  private void enterAddress(SelenideElement addressElement, String searchAddress) {
    addressElement.setValue(searchAddress.equals("Praha") ? "Praha, Česko" : searchAddress);
    sleep(ADDRESS_AUTOCOMPLETE_WAIT_MILLIS);
    addressElement.sendKeys(Keys.ENTER);
  }

  private void setPrice(WatchDogEntity watchDog) {
    String priceTo = watchDog.getPriceTo();
    String priceFrom = watchDog.getPriceFrom();
    if (!StringUtils.isEmpty(priceFrom) || !StringUtils.isEmpty(priceTo)) {
      $x("//button[contains(@class,'SearchPanel')]/span[text()='Cena']").click();
      if (!StringUtils.isEmpty(priceFrom)) {
        $x("//input[@placeholder='Cena od']").setValue(priceFrom);
      }
      if (!StringUtils.isEmpty(priceTo)) {
        $x("//input[@placeholder='Cena do']").setValue(priceTo);
      }
    }
  }

  public List<WatchDogEstatesEntity> getLatestOffers(
      Set<String> existingOfferIds, WatchDogEntity watchDog) {
    var offers = $$x("//article[contains(@class,'Property')]");
    List<WatchDogEstatesEntity> newOffers = new ArrayList<>();
    List<String> incompleteOfferIds = new ArrayList<>();
    for (int i = 0; i < offers.size(); i++) {
      SelenideElement offer = offers.get(i);
      String offerId =
          Pattern.compile("/(\\d+)-")
              .matcher(offer.$x(".//div/h2/a").getAttribute("href"))
              .results()
              .map(m -> m.group(1))
              .findFirst()
              .get();
      if (!existingOfferIds.contains(offerId) && !StringUtils.isEmpty(offerId)) {
        var newOffer = getNewOfferDetails(offer, watchDog, offerId);
        if (newOffer != null) {
          newOffers.add(newOffer);
        } else {
          incompleteOfferIds.add(offerId);
        }
      }
      if (i == 20) break;
    }
    if (!incompleteOfferIds.isEmpty()) {
      log.warn(
          "Skipped {} incomplete Bezrealitky offer(s) for watchdog {}: {}",
          incompleteOfferIds.size(),
          watchDog.getId(),
          incompleteOfferIds);
    }
    return newOffers;
  }

  private WatchDogEstatesEntity getNewOfferDetails(
      SelenideElement offer, WatchDogEntity watchDog, String offerId) {
    String url = offer.$x(".//h2/a").getAttribute("href");
    String header = offer.$x(".//h2/a/span[contains(@class,'propertyCardLabel')]").getText();
    String address = offer.$x(".//h2/a/span[contains(@class,'propertyCardAddress')]").getText();
    String price =
        extractPrimaryPriceNumber(
            offer.$x(".//span[contains(@class,'propertyPriceAmount')]").getText());
    executeJavaScript("window.scrollTo(0, 0);");
    if (StringUtils.isEmpty(header) || StringUtils.isEmpty(address) || StringUtils.isEmpty(price)) {
      return null;
    }

    var newOffer =
        new WatchDogEstatesEntity(
            null,
            watchDog,
            "BEZREALITKY.CZ",
            offerId,
            header,
            address,
            price,
            null,
            null,
            watchDog.getEmailReportFrequency().equals(WEEKLY)
                ? TRUE
                : !watchDog.getIsFirstSyncCompleted(),
            !watchDog.getIsFirstSyncCompleted(),
            Boolean.FALSE,
            !watchDog.getEmailReportFrequency().equals(WEEKLY),
            Boolean.FALSE,
            url,
            null,
            LocalDate.now(),
            null,
            null);

    newOffer = openNewTabAndVerify(newOffer);
    return newOffer;
  }

  private WatchDogEstatesEntity openNewTabAndVerify(WatchDogEstatesEntity newOffer) {
    ((JavascriptExecutor) getWebDriver()).executeScript("window.open()");
    ArrayList<String> tabs = new ArrayList<>(getWebDriver().getWindowHandles());
    getWebDriver().switchTo().window(tabs.get(tabs.size() - 1));
    open(newOffer.getOfferUrl());
    String imgUrl = null;
    try {
      imgUrl = $x("//a[contains(@href,'api.bezrealitky')]").getAttribute("href");
    } catch (ElementNotFound ignored) {
    }
    newOffer.setMainPicUrl(imgUrl);
    getWebDriver().close();
    getWebDriver().switchTo().window(tabs.getFirst());
    return newOffer;
  }
}
