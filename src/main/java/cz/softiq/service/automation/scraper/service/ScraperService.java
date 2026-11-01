package cz.softiq.service.automation.scraper.service;

import cz.softiq.service.automation.scraper.entity.ColdCallsEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScraperService {
  private final ScraperComponent scraperComponent;

  Iterable<ColdCallsEntity> getAllClients() {
    return scraperComponent.getAllClients();
  }

  void saveAllClients(Iterable<ColdCallsEntity> coldCallsEntities) {
    scraperComponent.saveAllClients(coldCallsEntities);
  }

  ColdCallsEntity findByEmail(String email) {
    return scraperComponent.findByEmail(email);
  }

  void contactIfNew(String email, String source) {
    scraperComponent.contactIfNew(email, source);
  }

  void contactScraped(Integer numberToContact) {
    scraperComponent.contactScraped(numberToContact);
  }
}
