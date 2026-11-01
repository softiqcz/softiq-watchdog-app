package cz.softiq.service.automation.offerSync.service.rest;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class RestApiService {
  private final RestApiWorkflowComponent component;

  String getToken(String url) {
    return component.getToken(url);
  }

  List<Integer> getActiveOffers(String clientName) {
    return component.getActiveOffers(clientName);
  }

  List<JsonNode> getActiveOfferDetails(String clientName) {
    return component.getActiveOfferDetails(clientName);
  }

  int postJson(String jsonDocumentAsString, String token, String url) throws Exception {
    return component.postJson(jsonDocumentAsString, token, url);
  }

  boolean markAsSoldJson(Integer offerId, String token, String url) {
    return component.markAsSoldJson(offerId, token, url);
  }
}
