package cz.softiq.service.automation.offerSync.service.rest;

import static io.restassured.RestAssured.given;

import com.fasterxml.jackson.databind.JsonNode;
import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.core.email.CoreLogEntity;
import cz.softiq.core.service.ICoreSoftiqService;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestApiWorkflowComponent implements IBaseService, IBeanProvider, ICoreSoftiqService {
  private final RestApiComponent restApiComponent;

  @Value("${spring.mail.username}")
  private String username;

  @Value("${secret.automation.password}")
  private String password;

  public String getToken(String url) {
    Response response =
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", username, "password", password))
            .when()
            .post(url + "/api/v2/public/auth/login");

    if (response.statusCode() != 200) {
      throw new SoftiqException(
          "Token generation failed. Status: "
              + response.statusCode()
              + " Body: "
              + response.asString());
    }
    return response.jsonPath().getString("data.token");
  }

  public List<Integer> getActiveOffers(String clientName) {

    return getActiveOfferDetails(clientName).stream()
        .map(offer -> offer.path("id").asInt())
        .filter(id -> id > 0)
        .toList();
  }

  public List<JsonNode> getActiveOfferDetails(String clientName) {

    Response response =
        given()
            .contentType(ContentType.JSON)
            .when()
            .get(clientName + "/api/v2/public/portfolio/offers");

    if (response.statusCode() != 200) {
      throw new SoftiqException(
          "Failed to get active offers. Status: "
              + response.statusCode()
              + " Body: "
              + response.asString());
    }

    JsonNode data = response.as(JsonNode.class).path("data");
    if (!data.isArray()) {
      return List.of();
    }

    List<JsonNode> activeOffers = new java.util.ArrayList<>();
    data.forEach(
        offer -> {
          if ("Aktivní".equalsIgnoreCase(offer.path("stav").asText())) {
            activeOffers.add(offer);
          }
        });
    return activeOffers;
  }

  public int postJson(String jsonDocumentAsString, String token, String url) throws Exception {
    Response response =
        given()
            .contentType("application/json")
            .header("Authorization", "Bearer " + token)
            .body(jsonDocumentAsString)
            .when()
            .post(url + "/api/v2/admin/portfolio");

    if (response.statusCode() == 200 || response.statusCode() == 201) {
      log.info("Offer updated sucessfully");
    } else {
      log.error(jsonDocumentAsString);
      String error = "Offer update failed " + response.statusCode();
      CoreLogEntity logEntity =
          new CoreLogEntity(null, LocalDate.now(), LocalDateTime.now(), error);
      saveLog(logEntity);
      log.error(error);
    }
    return restApiComponent.getIdFromJson(jsonDocumentAsString, response.asString());
  }

  public boolean markAsSoldJson(Integer offerId, String token, String url) {
    Map<String, Object> requestBody =
        Map.of(
            "email", username,
            "token", token,
            "data", Map.of("id", offerId));

    Response response =
        given()
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .body(requestBody)
            .when()
            .post(url + "/api/v2/admin/portfolio/markAsSold");

    if (response.statusCode() == 200 || response.statusCode() == 201) {
      log.info("Offer marked as sold successfully: {}", offerId);
      return true;
    }

    String error =
        String.format(
            "Offer failed to mark as sold: %d status:%d body:%s",
            offerId, response.statusCode(), response.asString());

    CoreLogEntity logEntity = new CoreLogEntity(null, LocalDate.now(), LocalDateTime.now(), error);

    saveLog(logEntity);
    log.error(error);
    throw new SoftiqException(error);
  }
}
