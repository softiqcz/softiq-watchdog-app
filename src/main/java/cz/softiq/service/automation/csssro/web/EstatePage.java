package cz.softiq.service.automation.csssro.web;

import static com.codeborne.selenide.Selenide.*;
import static cz.softiq.service.automation.csssro.model.JsonCreator.createJsonDocument;

import com.codeborne.selenide.*;
import cz.softiq.common.base.BaseBrowserClass;
import cz.softiq.service.automation.csssro.model.EstateOfferBuilder;
import cz.softiq.service.automation.csssro.model.FotkaBuilder;
import cz.softiq.service.automation.csssro.model.TextFormatter;
import cz.softiq.service.automation.offerSync.service.rest.IRestApiService;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EstatePage extends BaseBrowserClass implements IRestApiService {
  private final TextFormatter formatter = new TextFormatter();

  /**
   * Method takes url of a offer and gets all details
   *
   * @param offerUrl
   * @param isSoldOffer
   * @return
   */
  public String getOfferDetails(
      SelenideElement offerUrl, Boolean isSoldOffer, String token, String username) {
    String linkUrl = offerUrl.getAttribute("href");
    String offerCardPriceLine = getPriceLineFromOfferCard(offerUrl);
    // Use JavaScript to open a new window with the specified URL
    executeJavaScript("window.open(arguments[0])", linkUrl);
    // Switch to the new window if needed
    Selenide.switchTo().window(1);

    var lastExported = new Date().toInstant();
    String cena = getOfferPrice(offerCardPriceLine);

    String typSmlouvy =
        getElementText(
            $x("//div[@data-e2e='desktop-detail-navigation-bar']//ol/li[a][2]//a")); // e.g.
    // "Prodej" or "Pronájem"

    // text generation
    String uzitna =
        getElementText(
            $x("//dt[text()='Plocha']/following-sibling::dd//div[contains(.,'Užitná plocha')]"));
    String zastavena =
        getElementText(
            $x("//dt[text()='Plocha']/following-sibling::dd//div[contains(.,'Zastavěná plocha')]"));
    String celkova =
        getElementText(
            $x("//dt[text()='Plocha']/following-sibling::dd//div[contains(.,'Celková plocha')]"));
    String full = $x("//h1[@data-e2e='detail-heading']").getText();

    String[] parts = full.split("\\n");

    String header = parts[0].trim();
    String address = parts[1].trim();

    EstateOfferBuilder estateOffer =
        EstateOfferBuilder.builder()
            .mainHeadline(header)
            .castka(cena.isEmpty() ? "0" : cena)
            .mena("Kč")
            .costOfLiving(getOfferDetailParameter("Poznámka k ceně"))
            .mainText(
                formatter.replaceLineBreaks(
                    getElementText($x("//div[@data-e2e='detail-description']"))))
            .ulice(address)
            .typSmlouvy(typSmlouvy)
            .lastExported(lastExported)
            .inzenyrskeSite(getOfferDetailParameter("Infrastruktura"))
            .datumAktualizace(getOfferDetailParameter("Upraveno"))
            .zastaveno(
                zastavena.isEmpty()
                    ? ""
                    : String.valueOf(
                        (int) Math.ceil(Double.parseDouble(formatter.extractNumbers(zastavena)))))
            .uzitnaPlocha(formatter.extractNumbers(uzitna))
            .umisteniObjektu(getOfferDetailParameter("Lokalita"))
            .celkovaPlocha(formatter.extractNumbers(celkova))
            .stavba(getOfferDetailParameter("Stavba"))
            .kategorie(
                getElementText(
                    $x("//div[@data-e2e='desktop-detail-navigation-bar']//ol/li[a][1]//a")))
            .dispoziceBytu(
                getElementText(
                    $x("//div[@data-e2e='desktop-detail-navigation-bar']//ol/li[a][3]//a")))
            .vlastnictvi(getOfferDetailParameter("Vlastnictví"))
            .trida(getOfferDetailParameter("Energetická náročnost"))
            .stav(
                isSoldOffer
                    ? typSmlouvy.equals("Prodej")
                        ? "Prodáno"
                        : typSmlouvy.equals("Pronájem") ? "Pronajato" : "Zrealizováno"
                    : "Aktivní")
            .enabled(true)
            .build();

    // get Fotky
    $x("//div//img[starts-with(@alt,'" + typSmlouvy + "')]").click(ClickOptions.usingJavaScript());
    $x("//button[@data-e2e='gallery-image']/img").click();

    ElementsCollection pics = $$x("//button[@data-e2e='gallery-image']/img");
    int poradi = 1;
    List<FotkaBuilder> fotkyElements = new ArrayList<>();

    for (SelenideElement pic : pics) {
      String src = pic.getAttribute("srcset");
      if (src == null || src.isEmpty()) continue;

      boolean hlavni = false;
      if (poradi == 1) {
        hlavni = true;
      }
      FotkaBuilder fotkaBuilder =
          FotkaBuilder.builder().url(src).hlavni(hlavni).poradi(poradi).build();
      fotkyElements.add(fotkaBuilder);
      poradi++;
    }
    estateOffer.setFotka(fotkyElements);

    String document = createDocument(estateOffer, token, username);
    // close tab
    Selenide.closeWindow();
    Selenide.switchTo().window(0);
    return document;
  }

  private String createDocument(EstateOfferBuilder estateOffer, String token, String username) {
    try {
      return createJsonDocument(estateOffer, token, username);
    } catch (Exception e) {
      log.error("Failed to create offer document", e);
      return null;
    }
  }

  private String getElementText(SelenideElement selenideElement) {
    try {
      return selenideElement.exists() ? selenideElement.getText() : "";
    } catch (Throwable e) {
      return "";
    }
  }

  private String getOfferDetailParameter(String parameter) {
    try {
      return getElementText(
          $x(String.format("//dt[text()='%s']/following-sibling::dd", parameter)));
    } catch (Throwable e) {
      return null;
    }
  }

  private String getOfferPrice(String offerCardPriceLine) {
    String cena = formatter.extractNumbers(getOfferDetailParameter("Cena"));
    if (!cena.isEmpty()) {
      return cena;
    }

    return formatter.extractNumbers(offerCardPriceLine);
  }

  private String getPriceLineFromOfferCard(SelenideElement offerUrl) {
    String offerText = getElementText(offerUrl);
    for (String line : offerText.split("\\R")) {
      if (line.contains("Kč") || line.contains("CZK") || line.contains("€")) {
        return line;
      }
    }
    return "";
  }
}
