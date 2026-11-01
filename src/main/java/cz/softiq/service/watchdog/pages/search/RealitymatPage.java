package cz.softiq.service.watchdog.pages.search;

import static com.codeborne.selenide.Selenide.*;
import static cz.softiq.service.watchdog.model.enums.RealityEnums.REALITYMAT;
import static cz.softiq.service.watchdog.model.enums.SettingsEnums.FrequencyEnum.WEEKLY;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

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
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogAutomationService;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogOfferService;
import cz.softiq.service.watchdog.entity.WatchDogBuilder;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
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
import org.openqa.selenium.Keys;

@Slf4j
public class RealitymatPage
    implements IWatchDogAutomationService,
        IWatchDogOfferService,
        IBeanProvider,
        ICoreSoftiqService,
        IBaseService {

  public WatchDogBuilder doRealitymatSearch(
      WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    if (StringUtils.isEmpty(watchDog.getUrlRealitymat()) || watchDog.getDoResetSearch()) {
      open("https://www.realitymat.cz");
      acceptCookies();
      $x("//h1[text()='Začněte hledat nemovitost']")
          .should(Condition.visible, Duration.ofSeconds(10));
      String location = watchDog.getExactAddress();
      $x("//span[text()='Zadejte lokalitu']").click();
      var adressField = $x("//input[@type='search']").should(Condition.appear).setValue(location);
      SelenideElement adressInput = $x("//span[@class='select2-results']//ul/li");

      adressInput.scrollTo().should(Condition.appear).click();
      if (adressInput.is(Condition.visible)) {
        adressInput.click();
        try {
          adressInput.should(Condition.disappear);
        } catch (ElementShould e) {
          adressField.sendKeys(Keys.ENTER);
          adressInput.should(Condition.disappear);
        }
      } else {
        adressInput.should(Condition.disappear);
      }

      $x("//select[@id='a-af']").selectOption(watchDog.getOfferType());
      $x("//select[@id='a-at']").selectOption(watchDog.getEstateType());
      $x("//button[@type='submit']").click();

      try {
        $x("//span[text()='Nejnovější']").should(Condition.visible, Duration.ofSeconds(10));
      } catch (Throwable t) {
        $x("//div[text()='Nenalezeny žádné záznamy.']")
            .should(Condition.visible, Duration.ofSeconds(10));
      }

      boolean hasSettings =
          !StringUtils.isEmpty(watchDog.getFlatCategory())
              || !StringUtils.isEmpty(watchDog.getEstateCondition())
              || watchDog.getPriceFrom() != null
              || watchDog.getPriceTo() != null
              || watchDog.getUsableAreaFrom() != null
              || watchDog.getUsableAreaTo() != null;

      if (hasSettings) { // has some settings
        $x("//button[contains(normalize-space(.), 'Upravit hledání')]").click();

        // set category
        selectOptions(watchDog.getFlatCategory(), "Kategorie");

        // set condition
        if (!watchDog.getEstateType().equals("Pozemky")) {
          selectOptions(watchDog.getEstateCondition(), "Stav objektu");
        }
        // price
        setInput("p", watchDog.getPriceTo(), watchDog.getPriceFrom());
        // usable area
        String value = watchDog.getEstateType().equals("Pozemky") ? "ea" : "ua";
        setInput(value, watchDog.getUsableAreaTo(), watchDog.getUsableAreaFrom());
        $x("//button[@type='submit']").click();
      }
      watchDogBuilder.setIsChangedUrl(true);
      saveUrl(watchDog, TRUE, REALITYMAT, watchDogBuilder);
    } else {
      open(watchDog.getUrlRealitymat());
      acceptCookies();
    }
    try {
      $x("//span[text()='Nejnovější']").should(Condition.visible, Duration.ofSeconds(10));
      watchDogBuilder.setHasOffers(true);
    } catch (ElementNotFound | ElementShould e) {
      try {
        $x("//div[contains(., 'Zobrazeno')]/b[1][text()='1-10']").should(Condition.visible);
        watchDogBuilder.setHasOffers(true);
      } catch (ElementNotFound | ElementShould e2) {
        $x("//div[contains(@class,'alert') and text()='Nenalezeny žádné záznamy.']")
            .should(Condition.visible);
      }
    }
    return watchDogBuilder;
  }

  private void acceptCookies() {
    var cookieButton = $x("//button[@id='accept-btn']");
    try {
      cookieButton.should(Condition.visible, Duration.ofSeconds(10));
      cookieButton.click();
    } catch (Throwable e) {
      log.info("No cookies on realitymat");
    }
  }

  private void setInput(String value, String valueTo, String valueFrom) {
    if (!StringUtils.isEmpty(value)) {
      $x("//input[@name='a[t" + value + "]']").setValue(valueTo);
    }
    if (!StringUtils.isEmpty(value)) {
      $x("//input[@name='a[f" + value + "]']").setValue(valueFrom);
    }
  }

  private void selectOptions(String typeList, String labelText) {
    if (!StringUtils.isBlank(typeList)) {
      String[] types = typeList.split(";\\s*");
      try {
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
        throw new SoftiqException(error + " " + e.getMessage());
      }
    }
  }

  private static void setTypeOfLabelText(String labelText, String[] types) {
    for (String type : types) {
      if (!StringUtils.isEmpty(type)) {
        SelenideElement element =
            $x(
                String.format(
                    "//div[text()='%s']/ancestor::div[2]//label[text()='%s']/preceding-sibling::input[1]",
                    labelText, type));
        element.click();
      }
    }
  }

  public List<WatchDogEstatesEntity> getLatestOffers(
      Set<String> existingOfferIds, WatchDogEntity watchDog) {
    List<WatchDogEstatesEntity> newOffers = new ArrayList<>();
    ElementsCollection offers = $$x("//div[contains(@class,'row no-gutters')]");
    for (int i = 0; i < offers.size(); i++) {
      SelenideElement offer = offers.get(i).shouldBe(Condition.visible);
      String offerUrl = offer.$x(".//a").getAttribute("href");

      Pattern pattern = Pattern.compile(".*/detail/(\\d+)/.*");
      Matcher matcher = pattern.matcher(offerUrl);

      String offerId = null;
      if (matcher.matches()) {
        offerId = matcher.group(1);
      }

      WatchDogEstatesEntity newOffer = null;
      if (!existingOfferIds.contains(offerId) && !StringUtils.isEmpty(offerId)) {
        newOffer = getOfferDetails(offer, watchDog, offerId, offerUrl);
      }
      if (newOffer != null) {
        newOffers.add(newOffer);
      }
    }
    return newOffers;
  }

  private WatchDogEstatesEntity getOfferDetails(
      SelenideElement offer, WatchDogEntity watchDog, String offerId, String url) {
    String origin = "REALITYMAT.CZ";
    String header = offer.$x(".//h2").getText();
    String address = offer.$x(".//p").getText();
    String price =
        extractPrimaryPriceNumber(
            offer.$x(".//div[contains(@class,'lead font-weight-bold')]").getText());
    SelenideElement mainPic = offer.$x(".//img");
    String imgUrl = getImgUrl(mainPic);

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
            FALSE,
            TRUE,
            FALSE,
            FALSE,
            FALSE,
            url,
            imgUrl,
            LocalDate.now(),
            null,
            null);

    // for first sync, always skip notification
    boolean notifyClient = watchDog.getIsFirstSyncCompleted();
    Boolean doHideForEver = FALSE;
    Boolean isFirstSyncCompleted = Boolean.valueOf(notifyClient);

    switch (watchDog.getSendOfferAs()) {
      case UPDATED:
        notifyClient = FALSE;
        doHideForEver = TRUE;
        break;
      case NEW:
        notifyClient = TRUE;
        doHideForEver = FALSE;
        break;
      case ALL:
        notifyClient = TRUE;
        doHideForEver = FALSE;
        break;
    }

    newOffer.setIsUpdated(FALSE);

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

  private String getImgUrl(SelenideElement mainPic) {
    String srcset = mainPic.getAttribute("srcset");

    String bestUrl = null;
    int bestWidth = 0;

    if (srcset != null && !srcset.isEmpty()) {
      String[] parts = srcset.split(",");
      for (String part : parts) {
        String[] urlAndSize = part.trim().split(" ");
        if (urlAndSize.length == 2) {
          String url = urlAndSize[0].trim();
          String widthStr = urlAndSize[1].replace("w", "").trim();
          try {
            int width = Integer.parseInt(widthStr);
            if (width > bestWidth) {
              bestWidth = width;
              bestUrl = url;
            }
          } catch (NumberFormatException ignored) {
          }
        }
      }
    }

    if (bestUrl == null) {
      bestUrl = mainPic.getAttribute("src");
    }

    return bestUrl;
  }
}
