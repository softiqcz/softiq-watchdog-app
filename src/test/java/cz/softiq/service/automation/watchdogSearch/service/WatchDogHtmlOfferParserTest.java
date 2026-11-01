package cz.softiq.service.automation.watchdogSearch.service;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import cz.softiq.service.watchdog.entity.WatchDogEntity;
import java.util.Set;
import org.jsoup.Jsoup;
import org.testng.annotations.Test;

public class WatchDogHtmlOfferParserTest {

  private final WatchDogHtmlOfferParser parser = new WatchDogHtmlOfferParser();

  @Test
  public void usesFullSizeIdnesGalleryLink() {
    var detailDocument =
        Jsoup.parse(
            """
            <div class="b-gallery__img-lg">
              <div class="slick-track">
                <a href="https://sta-reality2.1gr.cz/full.jpg?gt=r" data-fancybox="images">
                  <img src="https://sta-reality2.1gr.cz/low-resolution.jpg">
                </a>
              </div>
            </div>
            """);

    assertEquals(
        parser.idnesDetailImageUrl(detailDocument), "https://sta-reality2.1gr.cz/full.jpg?gt=r");
  }

  @Test
  public void returnsNullWhenIdnesDetailHasNoGalleryImage() {
    assertNull(parser.idnesDetailImageUrl(Jsoup.parse("<div>No gallery</div>")));
  }

  @Test
  public void extractsAuthoritativeSrealityDetailPriceForMatchingOffer() {
    String pageSource =
        """
        <script id="__NEXT_DATA__" type="application/json">
          {
            "props": {
              "pageProps": {
                "listing": {
                  "id": 3091124300,
                  "name": "Prodej rodinného domu 177 m²",
                  "locality": {"city": "Hradec Králové"},
                  "priceSummaryCzk": 10700000
                },
                "similarListing": {
                  "id": 123456789,
                  "name": "Jiný dům",
                  "locality": {"city": "Praha"},
                  "priceSummaryCzk": 999000000
                }
              }
            }
          }
        </script>
        """;

    assertEquals(
        parser.extractSrealityDetailPrice(pageSource, "estate-list-item-3091124300"), "10700000");
  }

  @Test
  public void keepsSearchCardPriceWhenSrealityDetailPayloadIsUnavailable() {
    assertEquals(parser.extractSrealityDetailPrice("<html>No payload</html>", "3091124300"), "");
  }

  @Test
  public void parsesMatchingSrealitySearchWithoutBrowser() {
    var offers =
        parser.parseSrealityOffers(
            srealitySearchPage("Prodej domů Praha"), watchdog("Praha"), Set.of());
    assertEquals(offers.size(), 1);
    assertEquals(offers.getFirst().getOfferId(), "estate-list-item-123");
    assertEquals(offers.getFirst().getAddress(), "Praha");
  }

  @Test
  public void ignoresCaseAndNormalizesSearchAddressWhitespace() {
    var offers =
        parser.parseSrealityOffers(
            srealitySearchPage("Prodej domů PRAHA&nbsp; 6"), watchdog(" Praha  6 "), Set.of());
    assertEquals(offers.size(), 1);
  }

  @Test
  public void rejectsSearchResultsForDifferentLocation() {
    var error =
        expectThrows(
            IllegalStateException.class,
            () ->
                parser.parseSrealityOffers(
                    srealitySearchPage("Prodej domů Brno"), watchdog("Praha"), Set.of()));
    assertTrue(error.getMessage().contains("search title mismatch"));
    assertTrue(error.getMessage().contains("Praha"));
    assertTrue(error.getMessage().contains("Brno"));
  }

  @Test
  public void rejectsMissingSearchTitleBeforeParsingOffers() {
    var error =
        expectThrows(
            IllegalStateException.class,
            () ->
                parser.parseSrealityOffers(
                    Jsoup.parse("<html>No title</html>"), watchdog("Praha"), Set.of()));
    assertTrue(error.getMessage().contains("Missing Sreality search title"));
  }

  @Test
  public void rejectsMissingExpectedSearchAddress() {
    for (String address : new String[] {null, "", "  "}) {
      var error =
          expectThrows(
              IllegalStateException.class,
              () ->
                  parser.parseSrealityOffers(
                      srealitySearchPage("Prodej domů Praha"), watchdog(address), Set.of()));
      assertTrue(error.getMessage().contains("Missing expected Sreality search address"));
    }
  }

  @Test
  public void preservesEmptySearchResultHandlingWithoutTitleOrPayload() {
    var document = Jsoup.parse("<p>Je nám líto, s tímto filtrem nemáme žádné nemovitosti</p>");
    assertTrue(parser.parseSrealityOffers(document, watchdog("Praha"), Set.of()).isEmpty());
  }

  @Test
  public void preservesFilteringOfKnownOffers() {
    var offers =
        parser.parseSrealityOffers(
            srealitySearchPage("Prodej domů Praha"),
            watchdog("Praha"),
            Set.of("estate-list-item-123"));
    assertTrue(offers.isEmpty());
  }

  private WatchDogEntity watchdog(String address) {
    var watchDog = new WatchDogEntity();
    watchDog.setId(42);
    watchDog.setEmailReportFrequency(
        cz.softiq.service.watchdog.model.enums.SettingsEnums.FrequencyEnum.DAILY);
    watchDog.setSearchAddress(address);
    return watchDog;
  }

  private org.jsoup.nodes.Document srealitySearchPage(String title) {
    return Jsoup.parse(
        "<h1 data-e2e='search-title'>"
            + title
            + "</h1>"
            + """
        <script id="__NEXT_DATA__" type="application/json">
          {"props":{"pageProps":{"listings":[{
            "id":123,"name":"Prodej domu 100 m²",
            "locality":{"city":"Praha"},"priceSummaryCzk":5000000
          }]}}}
        </script>
        """);
  }
}
