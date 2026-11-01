package cz.softiq.service.automation.offerSync.service.rest;

import com.fasterxml.jackson.databind.JsonNode;
import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import java.util.List;

public interface IRestApiService extends IBeanProvider, IBaseService {
  private RestApiService getRestApiService() {
    return getBean(RestApiService.class);
  }

  default boolean markAsSoldJson(Integer offerId, String token, String url) {
    return getRestApiService().markAsSoldJson(offerId, token, url);
  }

  default int postJson(String jsonDocumentAsString, String token, String url) throws Exception {
    return getRestApiService().postJson(jsonDocumentAsString, token, url);
  }

  default List<Integer> getActiveOffersApi(String url) {
    return getRestApiService().getActiveOffers(url);
  }

  default List<JsonNode> getActiveOfferDetailsApi(String url) {
    return getRestApiService().getActiveOfferDetails(url);
  }

  default String getToken(String url) {
    return getRestApiService().getToken(url);
  }
}
