package cz.softiq.service.automation.scraper.service;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.service.automation.scraper.entity.ColdCallsEntity;

public interface IScraperService extends IBeanProvider, IBaseService {

  default Iterable<ColdCallsEntity> getAllClients() {
    return getService().getAllClients();
  }

  default void saveAllClients(Iterable<ColdCallsEntity> coldCallsEntities) {
    getService().saveAllClients(coldCallsEntities);
  }

  default ColdCallsEntity findByEmail(String email) {
    return getService().findByEmail(email);
  }

  default void contactIfNew(String email, String source) {
    getService().contactIfNew(email, source);
  }

  default void contactScraped(Integer numberToContact) {
    getService().contactScraped(numberToContact);
  }

  private ScraperService getService() {
    return getBean(ScraperService.class);
  }
}
