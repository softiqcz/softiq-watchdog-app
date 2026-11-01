package cz.softiq.service.automation.offerSync.service.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ActiveOfferMatcher {
  private ActiveOfferMatcher() {}

  public static OptionalInt addMatchingActiveOfferId(
      ObjectNode requestData, List<JsonNode> activeOffers) {
    List<JsonNode> matches =
        activeOffers.stream().filter(offer -> matches(requestData, offer)).toList();

    if (matches.isEmpty()) {
      return OptionalInt.empty();
    }

    int id = matches.stream().mapToInt(offer -> offer.path("id").asInt(0)).max().orElse(0);
    if (id <= 0) {
      return OptionalInt.empty();
    }

    if (matches.size() > 1) {
      log.warn(
          "Found {} duplicate active offers for '{}'; updating newest id {} so older duplicates can be marked sold",
          matches.size(),
          text(requestData, "mainHeadline"),
          id);
    }

    requestData.put("id", id);
    return OptionalInt.of(id);
  }

  private static boolean matches(JsonNode incoming, JsonNode active) {
    if (!"aktivni".equals(normalize(text(active, "stav")))) {
      return false;
    }

    String incomingHeadline = normalize(text(incoming, "mainHeadline"));
    String activeHeadline = normalize(text(active, "nadpis_nemovitosti", "mainHeadline"));
    if (!incomingHeadline.isEmpty() && incomingHeadline.equals(activeHeadline)) {
      return true;
    }

    // Headings can occasionally be edited. In that case, require the same address plus at least
    // two additional stable attributes. Price is deliberately never part of offer identity.
    if (!sameNonBlank(incoming, active, List.of("ulice"), List.of("ulice", "adresa"))) {
      return false;
    }

    int matchingAttributes = 0;
    matchingAttributes +=
        sameNonBlank(incoming, active, List.of("typSmlouvy"), List.of("typSmlouvy")) ? 1 : 0;
    matchingAttributes +=
        sameNonBlank(incoming, active, List.of("kategorie"), List.of("typ_nemovitosti")) ? 1 : 0;
    matchingAttributes +=
        sameNonBlank(
                incoming,
                active,
                List.of("dispoziceBytu"),
                List.of("flat_kind", "house_kind", "dispoziceBytu"))
            ? 1
            : 0;
    matchingAttributes +=
        sameNonBlank(incoming, active, List.of("uzitnaPlocha"), List.of("usable_area")) ? 1 : 0;
    matchingAttributes +=
        sameNonBlank(incoming, active, List.of("celkovaPlocha"), List.of("total_area")) ? 1 : 0;
    return matchingAttributes >= 2;
  }

  private static boolean sameNonBlank(
      JsonNode first, JsonNode second, List<String> firstNames, List<String> secondNames) {
    String firstValue = normalize(text(first, firstNames.toArray(String[]::new)));
    String secondValue = normalize(text(second, secondNames.toArray(String[]::new)));
    return !firstValue.isEmpty() && firstValue.equals(secondValue);
  }

  private static String text(JsonNode node, String... names) {
    for (String name : names) {
      JsonNode value = node.get(name);
      if (value != null && !value.isNull() && !value.asText().isBlank()) {
        return value.asText();
      }
    }
    return "";
  }

  private static String normalize(String value) {
    String withoutAccents =
        Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    List<String> parts = new ArrayList<>();
    for (String part : withoutAccents.toLowerCase(Locale.ROOT).trim().split("\\s+")) {
      if (!part.isBlank()) {
        parts.add(part);
      }
    }
    return String.join(" ", parts);
  }
}
