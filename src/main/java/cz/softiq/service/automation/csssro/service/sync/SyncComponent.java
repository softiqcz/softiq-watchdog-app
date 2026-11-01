package cz.softiq.service.automation.csssro.service.sync;

import com.fasterxml.jackson.databind.JsonNode;
import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.core.service.ICoreSoftiqService;
import cz.softiq.service.automation.csssro.web.SrealityWeb;
import cz.softiq.service.automation.offerSync.service.rest.IRestApiService;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SyncComponent
    implements IBaseService, IRestApiService, IBeanProvider, ICoreSoftiqService {

  public List<Integer> getOffers(
      String token, String username, String url, List<JsonNode> activeOffers) {
    return new SrealityWeb().syncAllOffers(token, username, url, activeOffers);
  }

  public List<Integer> findMissingIds(List<Integer> inDatabase, List<Integer> onWeb) {
    List<Integer> missingIds = new ArrayList<>();
    if (inDatabase != null && onWeb != null) {
      for (Integer id : inDatabase) {
        if (!onWeb.contains(id)) {
          missingIds.add(id);
        }
      }
    }
    return missingIds;
  }
}
