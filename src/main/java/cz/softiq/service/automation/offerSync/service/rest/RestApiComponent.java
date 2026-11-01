package cz.softiq.service.automation.offerSync.service.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.softiq.common.base.model.IBaseComponent;
import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.core.service.ICoreSoftiqService;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Component
public class RestApiComponent
    implements IBaseComponent, IBeanProvider, ICoreSoftiqService, IBaseService {
  // todo redo to use private final RestTemplate restTemplate = new RestTemplate();
  public Integer getId(String documentString) throws Exception {
    ByteArrayInputStream input =
        new ByteArrayInputStream(documentString.getBytes(StandardCharsets.UTF_8));
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = builder.parse(input);
    NodeList idNabidkaList = document.getElementsByTagName("id_nabidka");
    Element idNabidkaElement = (Element) idNabidkaList.item(0);
    return Integer.valueOf(idNabidkaElement.getTextContent());
  }

  public Integer getIdFromJson(String requestBody, String responseBody) throws Exception {
    Integer responseId = getIdFromJsonBody(responseBody);
    if (responseId != null) {
      return responseId;
    }

    Integer requestId = getIdFromJsonBody(requestBody);
    return requestId == null ? 0 : requestId;
  }

  private Integer getIdFromJsonBody(String jsonBody) throws Exception {
    if (jsonBody == null || jsonBody.isBlank()) {
      return null;
    }

    JsonNode json = new ObjectMapper().readTree(jsonBody);
    JsonNode data = json.has("data") ? json.get("data") : json;
    JsonNode id = data.get("id");
    if (id == null) {
      id = data.get("id_nabidka");
    }
    return id != null && id.canConvertToInt() ? id.asInt() : null;
  }

  public String createBasicAuthHeader(String username, String password) {
    String credentials = username + ":" + password;
    return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
  }

  public HttpRequest createRequest(
      String apiUrl,
      String basicAuthHeader,
      String path,
      String method,
      String requestBody,
      String contentType) {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder().uri(URI.create(apiUrl + path)).header("Content-Type", contentType);
    if (basicAuthHeader != null) {
      builder.header("Authorization", basicAuthHeader);
    }

    switch (method) {
      case "GET":
        builder.GET();
        break;
      case "POST":
        builder.POST(HttpRequest.BodyPublishers.ofString(requestBody));
        break;
      case "PUT":
        builder.PUT(HttpRequest.BodyPublishers.noBody());
        break;
      case "DELETE":
        builder.DELETE();
        break;
    }
    return builder.build();
  }
}
