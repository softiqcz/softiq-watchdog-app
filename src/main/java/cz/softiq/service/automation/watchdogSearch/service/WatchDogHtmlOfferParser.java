package cz.softiq.service.automation.watchdogSearch.service;

import static cz.softiq.service.watchdog.model.enums.SettingsEnums.FrequencyEnum.WEEKLY;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class WatchDogHtmlOfferParser {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final int REQUEST_TIMEOUT_MILLIS = 30_000;
  private static final int MAX_OFFERS = 20;
  private static final String SREALITY_NO_RESULTS_MESSAGE =
      "Je nám líto, s tímto filtrem nemáme žádné nemovitosti";

  public List<WatchDogEstatesEntity> getSrealityOffers(
      WatchDogEntity watchDog, Set<String> existingOfferIds) {
    return parseSrealityOffers(
        fetchSreality(watchDog.getUrlSreality()), watchDog, existingOfferIds);
  }

  public List<WatchDogEstatesEntity> parseSrealityOffers(
      Document document, WatchDogEntity watchDog, Set<String> existingOfferIds) {
    if (isSrealityNoResults(document)) {
      return List.of();
    }
    validateSrealitySearchTitle(document, watchDog);
    JsonNode nextData = readNextData(document);
    List<JsonNode> estateNodes = new ArrayList<>();
    collectSrealityEstateNodes(nextData, estateNodes);

    List<WatchDogEstatesEntity> offers = new ArrayList<>();
    Set<String> seenOfferIds = new HashSet<>();
    for (JsonNode estateNode : estateNodes) {
      if (offers.size() >= MAX_OFFERS) {
        break;
      }
      String rawOfferId = text(estateNode.path("id"));
      String offerId = WatchDogOfferUtils.normalizeSrealityOfferId(rawOfferId);
      if (isKnownOffer(offerId, existingOfferIds, seenOfferIds)) {
        continue;
      }

      String header = text(estateNode.path("name"));
      String address = srealityAddress(estateNode.path("locality"));
      String price = price(estateNode.path("priceSummaryCzk"));
      String imageUrl = prefixedUrl(estateNode.path("images").path(0).path("url").asText(null));
      String offerUrl = srealityOfferUrl(estateNode, rawOfferId);
      offers.add(
          newOffer(watchDog, "SREALITY.CZ", offerId, header, address, price, offerUrl, imageUrl));
    }
    return offers;
  }

  private void validateSrealitySearchTitle(Document document, WatchDogEntity watchDog) {
    Element title = document.selectFirst("h1[data-e2e=search-title]");
    if (title == null) {
      throw new IllegalStateException("Missing Sreality search title in " + document.location());
    }
    SrealitySearchAddress.validate(title.text(), watchDog);
  }

  public String extractSrealityDetailPrice(String pageSource, String offerId) {
    if (StringUtils.isBlank(pageSource) || StringUtils.isBlank(offerId)) {
      return "";
    }

    String numericOfferId = offerId.replaceFirst("^(estate-list-item-|estate-|list-item-)", "");
    try {
      JsonNode nextData = readNextData(Jsoup.parse(pageSource));
      List<JsonNode> estateNodes = new ArrayList<>();
      collectSrealityEstateNodes(nextData, estateNodes);
      return estateNodes.stream()
          .filter(estateNode -> numericOfferId.equals(text(estateNode.path("id"))))
          .map(estateNode -> price(estateNode.path("priceSummaryCzk")))
          .filter(StringUtils::isNotBlank)
          .findFirst()
          .orElse("");
    } catch (IllegalStateException e) {
      log.warn("Failed to extract Sreality detail price for offer {}", offerId, e);
      return "";
    }
  }

  public List<WatchDogEstatesEntity> getBezrealitkyOffers(
      WatchDogEntity watchDog, Set<String> existingOfferIds) {
    Document document = fetch(watchDog.getUrlBezrealitky());
    JsonNode apolloCache =
        readNextData(document).path("props").path("pageProps").path("apolloCache");

    List<WatchDogEstatesEntity> offers = new ArrayList<>();
    Set<String> seenOfferIds = new HashSet<>();
    for (String advertRef : bezrealitkyAdvertRefs(apolloCache)) {
      if (offers.size() >= MAX_OFFERS) {
        break;
      }
      JsonNode advert = apolloCache.path(advertRef);
      String offerId = text(advert.path("id"));
      if (isKnownOffer(offerId, existingOfferIds, seenOfferIds)) {
        continue;
      }

      String uri = advert.path("uri").asText("");
      String header = bezrealitkyHeader(advert);
      String address = firstTextValue(advert, "address");
      String price = price(advert.path("price"));
      String imageUrl =
          resolveBezrealitkyImage(apolloCache, advert.path("mainImage").path("__ref").asText());
      String offerUrl =
          uri.isBlank()
              ? watchDog.getUrlBezrealitky()
              : "https://www.bezrealitky.cz/nemovitosti-byty-domy/" + uri;
      offers.add(
          newOffer(
              watchDog, "BEZREALITKY.CZ", offerId, header, address, price, offerUrl, imageUrl));
    }
    return offers;
  }

  public List<WatchDogEstatesEntity> getBazosOffers(
      WatchDogEntity watchDog, Set<String> existingOfferIds) {
    Document document = fetch(watchDog.getUrlBazos());
    List<WatchDogEstatesEntity> offers = new ArrayList<>();
    Set<String> seenOfferIds = new HashSet<>();
    for (Element offer : document.select("div.maincontent > div.inzeraty")) {
      if (offers.size() >= MAX_OFFERS) {
        break;
      }
      Element link = offer.selectFirst("a[href*=/inzerat/]");
      if (link == null) {
        continue;
      }
      String offerUrl = link.absUrl("href");
      String offerId = firstMatch(offerUrl, "/inzerat/(\\d+)(?:/|\\.php)");
      if (isKnownOffer(offerId, existingOfferIds, seenOfferIds)) {
        continue;
      }
      String header = text(offer.selectFirst("h2"));
      if (!matchesConfiguredFlatCategory(watchDog, header)) {
        continue;
      }
      String address = text(offer.selectFirst(".inzeratylok"));
      String price = cleanPrice(text(offer.selectFirst(".inzeratycena")));
      String imageUrl = absUrl(offer.selectFirst("img"), "src");
      offers.add(
          newOffer(watchDog, "BAZOS.CZ", offerId, header, address, price, offerUrl, imageUrl));
    }
    return offers;
  }

  public List<WatchDogEstatesEntity> getIdnesOffers(
      WatchDogEntity watchDog, Set<String> existingOfferIds) {
    Document document = fetch(watchDog.getUrlIdnesReality());
    List<WatchDogEstatesEntity> offers = new ArrayList<>();
    Set<String> seenOfferIds = new HashSet<>();
    for (Element offer : document.select(".c-products__item")) {
      if (offers.size() >= MAX_OFFERS) {
        break;
      }
      Element link = offer.selectFirst("a.c-products__link[href]");
      if (link == null) {
        continue;
      }
      String offerUrl = link.absUrl("href");
      String offerId = firstMatch(offerUrl, "/([^/?#]+)/?$");
      if (isKnownOffer(offerId, existingOfferIds, seenOfferIds) || offerUrl.contains("random")) {
        continue;
      }
      String header = text(offer.selectFirst(".c-products__title"));
      String address = text(offer.selectFirst(".c-products__info"));
      String price =
          cleanPrice(text(offer.selectFirst(".c-products__price strong, .c-products__price")));
      String imageUrl = idnesImageUrl(offer, offerUrl);
      offers.add(
          newOffer(
              watchDog, "reality.idnes.cz", offerId, header, address, price, offerUrl, imageUrl));
    }
    return offers;
  }

  public List<WatchDogEstatesEntity> getRealitymatOffers(
      WatchDogEntity watchDog, Set<String> existingOfferIds) {
    Document document = fetch(watchDog.getUrlRealitymat());
    List<WatchDogEstatesEntity> offers = new ArrayList<>();
    Set<String> seenOfferIds = new HashSet<>();
    for (Element offer : document.select("div.row.no-gutters")) {
      if (offers.size() >= MAX_OFFERS) {
        break;
      }
      Element link = offer.selectFirst("a[href*=/detail/]");
      if (link == null) {
        continue;
      }
      String offerUrl = link.absUrl("href");
      String offerId = firstMatch(offerUrl, ".*/detail/(\\d+)/.*");
      if (isKnownOffer(offerId, existingOfferIds, seenOfferIds)) {
        continue;
      }
      String header = text(offer.selectFirst("h2"));
      String address = text(offer.selectFirst("p"));
      String price = cleanPrice(text(offer.selectFirst(".lead.font-weight-bold")));
      String imageUrl = absUrl(offer.selectFirst("img"), "src");
      offers.add(
          newOffer(watchDog, "REALITYMAT.CZ", offerId, header, address, price, offerUrl, imageUrl));
    }
    return offers;
  }

  public List<WatchDogEstatesEntity> getCeskeRealityOffers(
      WatchDogEntity watchDog, Set<String> existingOfferIds) {
    Document document = fetch(watchDog.getUrlCeskereality());
    List<WatchDogEstatesEntity> offers = new ArrayList<>();
    Set<String> seenOfferIds = new HashSet<>();
    for (Element offer : document.select("article[data-uid]")) {
      if (offers.size() >= MAX_OFFERS) {
        break;
      }
      String offerId = offer.attr("data-uid");
      if (isKnownOffer(offerId, existingOfferIds, seenOfferIds)) {
        continue;
      }
      Element link = offer.selectFirst("h2 a[href]");
      String headerAndAddress = text(offer.selectFirst("h2"));
      String[] splitHeader = headerAndAddress.split("m²", 2);
      String header = splitHeader[0].trim();
      String address = splitHeader.length > 1 ? splitHeader[1].trim() : "";
      String price = extractCeskeRealityPrice(offer);
      String imageUrl = ceskeRealityImageUrl(offer);
      offers.add(
          newOffer(
              watchDog,
              "CESKEREALITY.CZ",
              offerId,
              header,
              address,
              price,
              link == null ? watchDog.getUrlCeskereality() : link.absUrl("href"),
              imageUrl));
    }
    return offers;
  }

  public String extractCeskeRealityPrice(Element offer) {
    return cleanPrice(text(offer.selectFirst(".i-estate__footer-price-value, h3.price")));
  }

  private Document fetch(String url) {
    try {
      return Jsoup.connect(asciiUrl(url))
          .userAgent("Mozilla/5.0")
          .referrer(originReferrer(url))
          .timeout(REQUEST_TIMEOUT_MILLIS)
          .get();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to fetch watchdog URL " + url, e);
    }
  }

  private Document fetchSreality(String url) {
    try {
      Connection.Response response =
          Jsoup.connect(asciiUrl(url))
              .userAgent("Mozilla/5.0")
              .referrer(originReferrer(url))
              .timeout(REQUEST_TIMEOUT_MILLIS)
              .ignoreHttpErrors(true)
              .execute();
      Document document = response.parse();
      if (response.statusCode() < 400
          || (response.statusCode() == 404 && isSrealityNoResults(document))) {
        return document;
      }
      throw new IllegalStateException(
          "Failed to fetch watchdog URL " + url + ": HTTP " + response.statusCode());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to fetch watchdog URL " + url, e);
    }
  }

  private boolean isSrealityNoResults(Document document) {
    return document.select("p").stream()
        .map(Element::text)
        .anyMatch(SREALITY_NO_RESULTS_MESSAGE::equals);
  }

  private JsonNode readNextData(Document document) {
    Element nextData = document.selectFirst("script#__NEXT_DATA__");
    if (nextData == null || nextData.data().isBlank()) {
      throw new IllegalStateException("Missing __NEXT_DATA__ in " + document.location());
    }
    try {
      return OBJECT_MAPPER.readTree(nextData.data());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to parse __NEXT_DATA__ in " + document.location(), e);
    }
  }

  private void collectSrealityEstateNodes(JsonNode node, List<JsonNode> estateNodes) {
    if (node == null || node.isMissingNode()) {
      return;
    }
    if (node.isObject()
        && node.has("priceSummaryCzk")
        && node.has("name")
        && node.has("locality")
        && node.has("id")) {
      estateNodes.add(node);
      return;
    }
    if (node.isObject()) {
      node.elements().forEachRemaining(child -> collectSrealityEstateNodes(child, estateNodes));
    } else if (node.isArray()) {
      node.forEach(child -> collectSrealityEstateNodes(child, estateNodes));
    }
  }

  private WatchDogEstatesEntity newOffer(
      WatchDogEntity watchDog,
      String origin,
      String offerId,
      String header,
      String address,
      String price,
      String offerUrl,
      String imageUrl) {
    boolean weekly = watchDog.getEmailReportFrequency().equals(WEEKLY);
    boolean firstSyncDone = Boolean.TRUE.equals(watchDog.getIsFirstSyncCompleted());
    return new WatchDogEstatesEntity(
        null,
        watchDog,
        origin,
        offerId,
        header,
        address,
        price,
        null,
        null,
        weekly ? Boolean.TRUE : !firstSyncDone,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.TRUE,
        Boolean.FALSE,
        offerUrl,
        imageUrl,
        LocalDate.now(),
        null,
        null);
  }

  private Set<String> bezrealitkyAdvertRefs(JsonNode apolloCache) {
    Set<String> advertRefs = new LinkedHashSet<>();
    JsonNode rootQuery = apolloCache.path("ROOT_QUERY");
    Iterator<Map.Entry<String, JsonNode>> rootFields = rootQuery.fields();
    while (rootFields.hasNext()) {
      Map.Entry<String, JsonNode> rootField = rootFields.next();
      if (!rootField.getKey().startsWith("listAdverts(")) {
        continue;
      }
      for (JsonNode listItem : rootField.getValue().path("list")) {
        String ref = listItem.path("__ref").asText("");
        if (ref.startsWith("Advert:")) {
          advertRefs.add(ref);
        }
      }
    }

    if (!advertRefs.isEmpty()) {
      return advertRefs;
    }

    Iterator<Map.Entry<String, JsonNode>> fields = apolloCache.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      if (field.getKey().startsWith("Advert:")) {
        advertRefs.add(field.getKey());
      }
    }
    return advertRefs;
  }

  private boolean isKnownOffer(
      String offerId, Set<String> existingOfferIds, Set<String> seenOfferIds) {
    return StringUtils.isBlank(offerId)
        || existingOfferIds.contains(offerId)
        || !seenOfferIds.add(offerId);
  }

  private String srealityAddress(JsonNode locality) {
    List<String> parts = new ArrayList<>();
    addIfPresent(parts, locality.path("street").asText(null));
    addIfPresent(parts, locality.path("city").asText(null));
    addIfPresent(parts, locality.path("cityPart").asText(null));
    return String.join(" - ", parts);
  }

  private String srealityOfferUrl(JsonNode estateNode, String offerId) {
    String offerType = srealitySlug(estateNode.path("categoryTypeCb").path("name").asText(""));
    String estateType = srealitySlug(estateNode.path("categoryMainCb").path("name").asText(""));
    String estateSubtype = srealitySlug(estateNode.path("categorySubCb").path("name").asText(""));
    String locality = srealityLocalitySlug(estateNode.path("locality"));
    List<String> parts = new ArrayList<>();
    addIfPresent(parts, offerType);
    addIfPresent(parts, estateType);
    addIfPresent(parts, estateSubtype);
    addIfPresent(parts, locality);
    addIfPresent(parts, offerId);
    return "https://www.sreality.cz/detail/" + String.join("/", parts);
  }

  private String srealityLocalitySlug(JsonNode locality) {
    List<String> parts = new ArrayList<>();
    addIfPresent(parts, locality.path("citySeoName").asText(null));
    addIfPresent(parts, locality.path("cityPartSeoName").asText(null));
    addIfPresent(parts, locality.path("streetSeoName").asText(null));
    return String.join("-", parts);
  }

  private String srealitySlug(String value) {
    if (StringUtils.isBlank(value)) {
      return "";
    }
    String normalizedValue =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT);
    if (normalizedValue.matches("\\d+\\+[a-z0-9]+")) {
      return normalizedValue;
    }

    return switch (value) {
      case "Prodej" -> "prodej";
      case "Pronájem" -> "pronajem";
      case "Dražby" -> "drazby";
      case "Byty" -> "byt";
      case "Domy" -> "dum";
      case "Pozemky" -> "pozemek";
      case "Komerční" -> "komercni";
      case "Ostatní" -> "ostatni";
      case "Kanceláře" -> "kancelare";
      case "Sklady" -> "sklady";
      case "Výroba" -> "vyroba";
      case "Obchodní prostory" -> "obchodni-prostory";
      case "Ubytování" -> "ubytovani";
      case "Restaurace" -> "restaurace";
      default -> slugify(value);
    };
  }

  private String bezrealitkyHeader(JsonNode advert) {
    String offerType = advert.path("offerType").asText("").toLowerCase();
    String estateType = advert.path("estateType").asText("").toLowerCase();
    if (offerType.isBlank() && estateType.isBlank()) {
      return "";
    }
    return StringUtils.capitalize(offerType) + " " + estateType;
  }

  private boolean matchesConfiguredFlatCategory(WatchDogEntity watchDog, String header) {
    if (StringUtils.isBlank(watchDog.getFlatCategory())) {
      return true;
    }
    for (String category : watchDog.getFlatCategory().split(";")) {
      if (StringUtils.isNotBlank(category) && header.contains(category.trim())) {
        return true;
      }
    }
    return false;
  }

  private String resolveBezrealitkyImage(JsonNode apolloCache, String imageRef) {
    if (StringUtils.isBlank(imageRef)) {
      return null;
    }
    JsonNode image = apolloCache.path(imageRef);
    Iterator<Map.Entry<String, JsonNode>> fields = image.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      if (field.getKey().startsWith("url(")) {
        return field.getValue().asText(null);
      }
    }
    return null;
  }

  private String firstTextValue(JsonNode node, String fieldPrefix) {
    Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      if (field.getKey().startsWith(fieldPrefix)) {
        return field.getValue().asText("");
      }
    }
    return "";
  }

  private String text(JsonNode node) {
    return node == null || node.isMissingNode() || node.isNull() ? "" : node.asText("");
  }

  private String text(Element element) {
    return element == null ? "" : element.text().trim();
  }

  private String price(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return "";
    }
    return cleanPrice(node.asText());
  }

  private String cleanPrice(String value) {
    if (value == null) {
      return "";
    }
    return value.replaceAll("[^0-9]", "");
  }

  private String absUrl(Element element, String attribute) {
    return element == null ? null : element.absUrl(attribute);
  }

  private String ceskeRealityImageUrl(Element offer) {
    Element image = offer.selectFirst("picture img[src]");
    if (image == null) {
      image = offer.selectFirst("img:not(.blurred)[src]");
    }
    if (image == null) {
      image = offer.selectFirst("img[src]");
    }

    String imageUrl = absUrl(image, "src");
    if (imageUrl == null) {
      return null;
    }
    return imageUrl
        .replace("/32x32_webp/", "/320x320_webp/")
        .replace("/32x32_jpg/", "/320x320_jpg/");
  }

  private String idnesImageUrl(Element offer, String offerUrl) {
    String fallbackImageUrl = normalizeEscapedUrl(firstMatch(offer.html(), "url\\('([^']+)'\\)"));
    try {
      String detailImageUrl = idnesDetailImageUrl(fetch(offerUrl));
      return StringUtils.isNotBlank(detailImageUrl) ? detailImageUrl : fallbackImageUrl;
    } catch (RuntimeException e) {
      log.warn("Failed to fetch full-size Reality.iDNES image from {}", offerUrl, e);
      return fallbackImageUrl;
    }
  }

  public String idnesDetailImageUrl(Document detailDocument) {
    Element imageLink =
        detailDocument.selectFirst(
            ".b-gallery__img-lg a[data-fancybox=images][href], "
                + ".slick-track a[data-fancybox=images][href]");
    return imageLink == null ? null : normalizeEscapedUrl(imageLink.absUrl("href"));
  }

  private String firstMatch(String input, String regex) {
    if (input == null) {
      return "";
    }
    return Pattern.compile(regex)
        .matcher(input)
        .results()
        .map(match -> match.group(1))
        .findFirst()
        .orElse("");
  }

  private String prefixedUrl(String url) {
    if (url == null || url.isBlank()) {
      return null;
    }
    return url.startsWith("//") ? "https:" + url : url;
  }

  private String normalizeEscapedUrl(String url) {
    return prefixedUrl(
        Objects.toString(url, "").replace("\\/", "/").replace("\\:", ":").replace("\\.", "."));
  }

  private String slugify(String value) {
    if (StringUtils.isBlank(value)) {
      return "";
    }
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase()
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("(^-|-$)", "");
  }

  private String asciiUrl(String url) {
    try {
      return new URI(url).toASCIIString();
    } catch (IllegalArgumentException | URISyntaxException e) {
      return url;
    }
  }

  private String originReferrer(String url) {
    try {
      URI uri = new URI(asciiUrl(url));
      String scheme = uri.getScheme();
      String host = uri.getHost();
      if (StringUtils.isBlank(scheme) || StringUtils.isBlank(host)) {
        return "https://www.google.com/";
      }
      int port = uri.getPort();
      return scheme + "://" + host + (port > -1 ? ":" + port : "") + "/";
    } catch (IllegalArgumentException | URISyntaxException e) {
      return "https://www.google.com/";
    }
  }

  private void addIfPresent(List<String> parts, String value) {
    if (StringUtils.isNotBlank(value) && !parts.contains(value)) {
      parts.add(value);
    }
  }
}
