package cz.softiq.service.watchdog.pages.search;

import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static cz.softiq.service.watchdog.model.enums.SettingsEnums.FrequencyEnum.WEEKLY;
import static java.lang.Boolean.TRUE;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogAutomationService;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogOfferService;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogPersistenceService;
import cz.softiq.service.watchdog.entity.WatchDogBuilder;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.JavascriptExecutor;

@Slf4j
public class BazosPage
    implements IWatchDogAutomationService, IWatchDogOfferService, IWatchDogPersistenceService {

  public void doSearch(String generatedUrl, WatchDogEntity watchDog) {
    open(generatedUrl);
    if (!StringUtils.isEmpty(watchDog.getExactAddress())) {
      $x("//input[@id='hlokalita']").setValue(watchDog.getExactAddress());
      $x("//table//tr[2]").click();
      if (!$x("//input[@id='hlokalita']").getValue().matches("\\d{5}")) {
        throw new SoftiqException("Bazos url entered incorrect");
      }
    } else {
      String error = "Only exact address is supported";
      throw new SoftiqException(error);
    }
  }

  public List<WatchDogEstatesEntity> getLatestOffers(
      Set<String> existingOfferIds, WatchDogEntity watchDog, WatchDogBuilder watchDogBuilder) {
    SelenideElement cookies = $x("//button[text()='Souhlasím']");
    if (cookies.exists()) cookies.click();
    List<WatchDogEstatesEntity> newOffers = new ArrayList<>();
    for (int z = 1; z < 3; z++) {
      $x("//*[text()='Zobrazeno']").should(Condition.exist, Duration.ofSeconds(10));
      var offers = $$x("//div[@class='maincontent']/div[contains(@class,'inzeraty')]");
      for (int i = 1; i < offers.size(); i++) { // skip the first one - its header
        SelenideElement offer =
            $$x("//div[@class='maincontent']/div[contains(@class,'inzeraty')]").get(i);
        String url = offer.$x(".//a").getAttribute("href");
        Pattern pattern = Pattern.compile("/inzerat/(\\d+)(/|\\.php)"); // Updated regex
        Matcher matcher = pattern.matcher(url);
        if (!matcher.find()) {
          throw new SoftiqException("No offer ID found in URL: " + url);
        }
        String offerId = matcher.group(1);
        if (!existingOfferIds.contains(offerId) && !StringUtils.isEmpty(offerId)) {
          watchDogBuilder = getNewOfferDetails(offer, watchDog, offerId, watchDogBuilder);
          var newOffer = watchDogBuilder.getOffer();
          if (newOffer != null) {
            newOffers.add(newOffer);
          }
        }
      }

      newOffers =
          newOffers.stream()
              .collect(
                  Collectors.collectingAndThen(
                      Collectors.toMap(
                          WatchDogEstatesEntity::getOfferId,
                          Function.identity(),
                          (existing, replacement) -> existing),
                      map -> new ArrayList<>(map.values())));

      // switch page
      var nextPage = $x("//b[text()='Další']");
      if (nextPage.exists()) nextPage.click();
    }

    return newOffers;
  }

  private WatchDogBuilder getNewOfferDetails(
      SelenideElement offer,
      WatchDogEntity watchDog,
      String offerId,
      WatchDogBuilder watchDogBuilder) {
    String url = offer.$x(".//a").getAttribute("href");
    String header = offer.$x(".//h2").getText();
    String address = offer.$x(".//div[@class='inzeratylok']").getText();
    String price = extractPrimaryPriceNumber(offer.$x(".//div[@class='inzeratycena']").getText());
    if (watchDog.getFlatCategory() != null) {
      String[] categories = watchDog.getFlatCategory().split(";");
      boolean matchFound = Arrays.stream(categories).anyMatch(header::contains);
      if (matchFound) {
        return getWatchDogEstatesEntity(
            watchDog, offerId, header, address, price, url, watchDogBuilder);
      }
    } else {
      return getWatchDogEstatesEntity(
          watchDog, offerId, header, address, price, url, watchDogBuilder);
    }
    return watchDogBuilder;
  }

  private WatchDogBuilder getWatchDogEstatesEntity(
      WatchDogEntity watchDog,
      String offerId,
      String header,
      String address,
      String price,
      String url,
      WatchDogBuilder watchDogBuilder) {
    String origin = "BAZOS.CZ";
    String imgUrlFallback =
        $x("//a[contains(@href,'inzerat/" + offerId + "')]/img").getAttribute("src");

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
            imgUrlFallback,
            LocalDate.now(),
            null,
            null);
    newOffer = openNewTabAndVerify(newOffer, watchDogBuilder);
    watchDogBuilder.setOffer(newOffer);
    return watchDogBuilder;
  }

  private WatchDogEstatesEntity openNewTabAndVerify(
      WatchDogEstatesEntity newOffer, WatchDogBuilder watchDogBuilder) {
    ((JavascriptExecutor) getWebDriver()).executeScript("window.open()");
    ArrayList<String> tabs = new ArrayList<>(getWebDriver().getWindowHandles());
    getWebDriver().switchTo().window(tabs.get(tabs.size() - 1));
    open(newOffer.getOfferUrl());
    var img = $x("//div[@class='carousel-cell']//img");
    String imgUrl;
    if (img.exists()) {
      imgUrl = img.getAttribute("src");
      newOffer.setMainPicUrl(imgUrl);
    }

    String offerText = $x("//div[@class='popisdetail']").getText().toLowerCase();
    List<String> keywords =
        List.of("modululání", "modul", "mobil", "mobilní", "projekt", "moře", "pláž", "pobřeží");

    boolean isInvalidOffer = keywords.stream().anyMatch(offerText::contains);

    watchDogBuilder.setIsInvalidOffer(isInvalidOffer);

    getWebDriver().close();
    getWebDriver().switchTo().window(tabs.getFirst());
    return newOffer;
  }
}
