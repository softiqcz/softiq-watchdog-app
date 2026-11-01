package cz.softiq.service.automation.csssro.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonCreator {

  public static String createJsonDocument(
      EstateOfferBuilder estateOffer, String token, String username)
      throws JsonProcessingException {
    Map<String, Object> data = new LinkedHashMap<>();
    putIfNotNull(data, "mainHeadline", estateOffer.getMainHeadline());
    putIfNotNull(data, "mainText", estateOffer.getMainText());
    putIfNotNull(data, "castka", estateOffer.getCastka());
    putIfNotNull(data, "mena", estateOffer.getMena());
    putIfNotNull(data, "costOfLiving", estateOffer.getCostOfLiving());
    putIfNotNull(data, "ulice", estateOffer.getUlice());
    putIfNotNull(data, "typSmlouvy", estateOffer.getTypSmlouvy());
    putIfNotNull(data, "inzenyrskeSite", estateOffer.getInzenyrskeSite());
    putIfNotNull(data, "datumAktualizace", estateOffer.getDatumAktualizace());
    putIfNotNull(data, "zastaveno", estateOffer.getZastaveno());
    putIfNotNull(data, "uzitnaPlocha", estateOffer.getUzitnaPlocha());
    putIfNotNull(data, "umisteniObjektu", estateOffer.getUmisteniObjektu());
    putIfNotNull(data, "celkovaPlocha", estateOffer.getCelkovaPlocha());
    putIfNotNull(data, "stavba", estateOffer.getStavba());
    putIfNotNull(data, "kategorie", estateOffer.getKategorie());
    putIfNotNull(data, "dispoziceBytu", estateOffer.getDispoziceBytu());
    putIfNotNull(data, "vlastnictvi", estateOffer.getVlastnictvi());
    putIfNotNull(data, "trida", estateOffer.getTrida());
    putIfNotNull(data, "stav", estateOffer.getStav());
    putIfNotNull(data, "enabled", estateOffer.getEnabled());
    putIfNotNull(
        data,
        "lastExported",
        estateOffer.getLastExported() == null ? null : estateOffer.getLastExported().toString());
    putIfNotNull(data, "fotky", estateOffer.getFotka());

    return new ObjectMapper()
        .writeValueAsString(
            Map.of(
                "email", username,
                "token", token,
                "data", data));
  }

  private static void putIfNotNull(Map<String, Object> target, String key, Object value) {
    if (value != null) {
      target.put(key, value);
    }
  }
}
