package cz.softiq.service.watchdog.pages.search;

import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static cz.softiq.service.watchdog.model.enums.SettingsEnums.FrequencyEnum.WEEKLY;
import static java.lang.Boolean.TRUE;

import com.codeborne.selenide.ClickOptions;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.ElementNotFound;
import com.codeborne.selenide.ex.ElementShould;
import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.core.email.CoreLogEntity;
import cz.softiq.core.service.ICoreSoftiqService;
import cz.softiq.service.automation.scraper.service.IScraperService;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogOfferService;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import cz.softiq.service.watchdog.model.enums.WatchDogEnum;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;

@Slf4j
public class IdnesPage
    implements IBeanProvider,
        ICoreSoftiqService,
        IBaseService,
        IScraperService,
        IWatchDogOfferService {

  public void doSearch(WatchDogEntity watchDog) {
    acceptCookies();
    $x("//h1[contains(text(),'Bydlete na správné adrese')]").shouldBe(Condition.visible);
    SelenideElement estateType = $x("//input[@name='typeId']/following-sibling::span");
    estateType.shouldBe(Condition.visible).click();
    estateType
        .$x(
            ".//span[contains(@onclick, '"
                + WatchDogEnum.fromString(watchDog.getEstateType()).getNameIdnes()
                + "')]")
        .click();

    SelenideElement offerType = $x("//input[@name='offerTypeId']/following-sibling::span");
    offerType.should(Condition.appear).click();
    offerType
        .$x(".//span[contains(text(), '" + watchDog.getOfferType().toLowerCase() + "')]")
        .click();

    $x("//input[@name='locality']").should(Condition.appear).setValue(watchDog.getSearchAddress());
    $x("//ul/li").should(Condition.appear).click();
    String inRange = watchDog.getInRange();
    if (!watchDog.getExactAddress().equals("Praha") && inRange.contains("km")) {
      SelenideElement range = $x("//span[text()='Přesně']");
      range.should(Condition.appear).click();
      if (inRange.contains("25")) {
        inRange = "20 km";
      }
      range
          .parent()
          .$x(
              ".//span[contains(text(), '"
                  + inRange.replaceAll("km", "Km").replaceAll(",", ".")
                  + "')]")
          .click();
    }

    $x("//button[@type='submit']").click();
    $x("//a[@href='#form-filter']").shouldBe(Condition.visible, Duration.ofSeconds(10)).click();

    SelenideElement saveButton = $x("//button[@value='Hledat']");
    saveButton.shouldBe(Condition.visible, Duration.ofSeconds(10));
    // dispozice
    SelenideElement flatCategory = $x("//p/strong[text()='Dispozice']").parent();
    SelenideElement homeCategory = $x("//p/strong[text()='Počet místností']").parent();

    if (watchDog.getEstateType().equals("Domy")) {
      selectInputs(convertFlatToHomeCategory(watchDog.getFlatCategory()), homeCategory);
    } else {
      selectOptions(watchDog.getFlatCategory(), flatCategory);
    }

    if (!watchDog.getEstateType().equals("Pozemky")) {
      SelenideElement condition = $x("//p/strong[text()='Stav nemovitosti']").parent().scrollTo();
      selectOptionsSpan(watchDog.getEstateCondition(), condition);
    }
    SelenideElement price = $x("//p/label[text()='Cena']").parent().parent();
    setInput(watchDog.getPriceFrom(), price.$x(".//input[@placeholder='Od:']"));
    setInput(watchDog.getPriceTo(), price.$x(".//input[@placeholder='Do:']"));

    if (!watchDog.getEstateType().equals("Pozemky")) {
      SelenideElement area = $x("//p/label[text()='Užitná plocha']").parent().parent();
      setInput(watchDog.getUsableAreaFrom(), area.$x(".//input[@placeholder='Od:']"));
      setInput(watchDog.getUsableAreaTo(), area.$x(".//input[@placeholder='Do:']"));
    } else {
      SelenideElement area = $x("//p/label[text()='Plocha pozemku']").parent().parent();
      setInput(watchDog.getUsableAreaFrom(), area.$x(".//input[@placeholder='Od:']"));
      setInput(watchDog.getUsableAreaTo(), area.$x(".//input[@placeholder='Do:']"));
    }

    saveButton.click();
    $x("//a[@href='/watchdog/']").shouldBe(Condition.visible, Duration.ofSeconds(20));
  }

  private String convertFlatToHomeCategory(String flatCategory) {
    if (flatCategory == null) {
      return null;
    }

    flatCategory = flatCategory.trim();

    Matcher matcher =
        Pattern.compile("^(\\d+)\\s*\\+\\s*(kk|\\d+)$", Pattern.CASE_INSENSITIVE)
            .matcher(flatCategory);

    if (!matcher.matches()) {
      return null;
    }

    int rooms = Integer.parseInt(matcher.group(1));

    // +1 (or +2, etc.) means an additional separate room.
    if (!"kk".equalsIgnoreCase(matcher.group(2))) {
      rooms++;
    }

    return rooms >= 6 ? "6 a více" : String.valueOf(rooms);
  }

  private void setInput(String amount, SelenideElement element) {
    if (amount != null) {
      executeJavaScript("arguments[0].scrollIntoView({block: 'center'});", element);
      element.setValue(amount);
    }
  }

  private void selectInputs(String typeList, SelenideElement element) {
    executeJavaScript("arguments[0].scrollIntoView({block: 'center'});", element);
    if (!StringUtils.isBlank(typeList)) {
      String[] types = typeList.split(";\\s*");
      try {
        setTypeOfLabelInput(element, types);
      } catch (ElementNotFound | ElementShould | ElementClickInterceptedException e) {
        String error = "Podmínka nenalezena " + typeList + " " + element + " " + e.getMessage();
        saveLog(new CoreLogEntity(null, LocalDate.now(), LocalDateTime.now(), error));
        log.error(error);
        throw new SoftiqException("Podmínka nenalezena");
      } catch (Throwable e) {
        String error =
            "Podmínka nemohla být zadána, zkouším znovu "
                + typeList
                + " "
                + element
                + " "
                + e.getMessage();
        saveLog(new CoreLogEntity(null, LocalDate.now(), LocalDateTime.now(), error));
        log.error(error);
        setTypeOfLabelInput(element, types);
      }
    }
  }

  private void selectOptions(String typeList, SelenideElement element) {
    executeJavaScript("arguments[0].scrollIntoView({block: 'center'});", element);
    if (!StringUtils.isBlank(typeList)) {
      String[] types = typeList.split(";\\s*");
      try {
        setTypeOfLabelText(element, types);
      } catch (ElementNotFound | ElementShould | ElementClickInterceptedException e) {
        String error = "Podmínka nenalezena " + typeList + " " + element + " " + e.getMessage();
        saveLog(new CoreLogEntity(null, LocalDate.now(), LocalDateTime.now(), error));
        log.error(error);
        throw new SoftiqException("Podmínka nenalezena");
      } catch (Throwable e) {
        String error =
            "Podmínka nemohla být zadána, zkouším znovu "
                + typeList
                + " "
                + element
                + " "
                + e.getMessage();
        saveLog(new CoreLogEntity(null, LocalDate.now(), LocalDateTime.now(), error));
        log.error(error);
        setTypeOfLabelText(element, types);
      }
    }
  }

  private void selectOptionsSpan(String typeList, SelenideElement element) {
    executeJavaScript("arguments[0].scrollIntoView({block: 'center'});", element);
    if (!StringUtils.isBlank(typeList)) {
      String[] types = typeList.split(";\\s*");
      try {
        setTypeOfLabelSpan(element, types);
      } catch (ElementNotFound | ElementShould | ElementClickInterceptedException e) {
        String error = "Podmínka nenalezena " + typeList + " " + element + " " + e.getMessage();
        saveLog(new CoreLogEntity(null, LocalDate.now(), LocalDateTime.now(), error));
        log.error(error);
        throw new SoftiqException("Podmínka nenalezena");
      } catch (Throwable e) {
        String error =
            "Podmínka nemohla být zadána, zkouším znovu "
                + typeList
                + " "
                + element
                + " "
                + e.getMessage();
        saveLog(new CoreLogEntity(null, LocalDate.now(), LocalDateTime.now(), error));
        log.error(error);
        setTypeOfLabelSpan(element, types);
      }
    }
  }

  private static void setTypeOfLabelText(SelenideElement parent, String[] types) {
    for (String type : types) {
      parent.$x(".//a[contains(text(),'" + type + "')]").click();
    }
  }

  private static void setTypeOfLabelInput(SelenideElement parent, String[] types) {
    for (String type : types) {
      parent.scrollTo().$x(".//label[input[@value='" + type + "']]").click();
    }
  }

  private static void setTypeOfLabelSpan(SelenideElement parent, String[] types) {
    for (String type : types) {
      try {
        parent
            .$x(".//span[span[@class='icon icon--check'] and contains(., '" + type + "')]")
            .click();
      } catch (ElementNotFound | ElementShould | ElementClickInterceptedException e) {
        log.info("Podminka not found " + type);
      }
    }
  }

  private void acceptCookies() {
    try {
      $x("//button[@id='didomi-notice-agree-button']")
          .should(Condition.visible, Duration.ofSeconds(10))
          .click();
    } catch (ElementNotFound | ElementShould e) {
      log.info("No cookies on idnes page");
    }
  }

  public List<WatchDogEstatesEntity> getLatestOffers(
      Set<String> existingOfferIds, WatchDogEntity watchDog) {
    SelenideElement offersParent = $x("//div[contains(@class,'c-products__list grid')]");
    if (offersParent.exists()) {
      ElementsCollection offers = loadProductCards();
      int size = offers.size();
      List<WatchDogEstatesEntity> newOffers = new ArrayList<>();
      for (int i = 0; i < size; i++) {
        SelenideElement offer = offers.get(i);
        SelenideElement link = offer.$("a.c-products__link[href]");
        if (!link.exists()) {
          continue;
        }
        String url = link.getAttribute("href");
        Pattern pattern = Pattern.compile("/([^/?#]+)/?$");
        Matcher matcher = pattern.matcher(url);
        if (!url.contains("random")
            && !url.startsWith("https://www.reality.idnes.cz")) { // its an ad
          if (!matcher.find()) {
            throw new SoftiqException("No offer ID found in URL: " + url);
          }
          String offerId = matcher.group(1);
          if (!existingOfferIds.contains(offerId) && !StringUtils.isEmpty(offerId)) {
            var newOffer = getNewOfferDetails(offer, watchDog, offerId);
            newOffers.add(newOffer);
          }
          if (newOffers.size() == 20) break;
        }
      }
      return newOffers;
    }
    return null;
  }

  private ElementsCollection loadProductCards() {
    ElementsCollection offers = $$(".c-products__item");
    int previousSize = -1;
    for (int i = 0; i < 5 && offers.size() != previousSize; i++) {
      previousSize = offers.size();
      if (!offers.isEmpty()) {
        executeJavaScript("arguments[0].scrollIntoView(false);", offers.last());
      }
      sleep(1000);
      offers = $$(".c-products__item");
    }
    return offers;
  }

  private WatchDogEstatesEntity getNewOfferDetails(
      SelenideElement offer, WatchDogEntity watchDog, String offerId) {
    String origin = "reality.idnes.cz";
    offer.scrollTo();
    String url = offer.$("a.c-products__link[href]").getAttribute("href");
    String header = offer.$(".c-products__title").getText();
    String address = offer.$(".c-products__info").getText();
    String price = extractPrimaryPriceNumber(offer.$(".c-products__price").getText());

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
    String imgUrl = $x("//div[@class='slick-track']/a").getAttribute("href");
    if (!StringUtils.contains(imgUrl, ".jpg")) {
      $x("//div[@class='b-gallery__imgs-sm']/a[2]")
          .shouldBe(Condition.visible, Duration.ofSeconds(10))
          .click(ClickOptions.usingJavaScript());
      imgUrl = $x("//img[@class='fancybox-image']").scrollIntoCenter().getAttribute("src");
    }
    newOffer.setMainPicUrl(imgUrl);
    String author = $x("//a[contains(@href,'mailto')]").getText();
    contactIfNew(author, newOffer.getOrigin());
    getWebDriver().close();
    getWebDriver().switchTo().window(tabs.getFirst());
    return newOffer;
  }
}
