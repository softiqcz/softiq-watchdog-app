package cz.softiq.service.automation.csssro.service.sync;

import com.fasterxml.jackson.databind.JsonNode;
import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import java.util.List;

public interface IOfferSyncService extends IBeanProvider, IBaseService {
  private SyncComponent getSyncComponent() {
    return getBean(SyncComponent.class);
  }

  default List<Integer> getOffers(
      String token, String username, String url, List<JsonNode> activeOffers) {
    return getSyncComponent().getOffers(token, username, url, activeOffers);
  }

  default List<Integer> findMissingIds(List<Integer> inDatabase, List<Integer> onWeb) {
    return getSyncComponent().findMissingIds(inDatabase, onWeb);
  }
}
