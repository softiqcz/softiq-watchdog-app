package cz.softiq.service.automation.scraper.service;

import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.service.automation.scraper.entity.ColdCallsEntity;
import cz.softiq.service.automation.scraper.repository.ScraperRepository;
import cz.softiq.service.softiq.service.email.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class ScraperComponent implements IEmailService {
  private final ScraperRepository scraperRepository;

  public Iterable<ColdCallsEntity> getAllClients() {
    return scraperRepository.findAll();
  }

  public void saveAllClients(Iterable<ColdCallsEntity> coldCallsEntities) {
    scraperRepository.saveAll(coldCallsEntities);
  }

  private void saveSingleClient(ColdCallsEntity coldCallsEntity) {
    scraperRepository.save(coldCallsEntity);
  }

  public ColdCallsEntity findByEmail(String email) {
    return scraperRepository.findByEmail(email);
  }

  public void contactIfNew(String email, String source) {
    var existingEntity = scraperRepository.findByEmail(email);
    if (existingEntity == null) {
      var author = new ColdCallsEntity("Unknown", email, source);
      author.setIsContacted(true);
      saveSingleClient(author);
      coldEmail(email);
    }
  }

  public void contactScraped(Integer numberToContact) {
    var clients = scraperRepository.findAllRandom(numberToContact);
    for (ColdCallsEntity client : clients) {
      if (client.getIsContacted()) {
        throw new SoftiqException("Client is already contacted!");
      }
      //      coldEmail("martin.svejda@vodafone.com");
      //      coldEmail("svobodova1petra@seznam.cz");
      //      coldEmail("svejdam@icloud.com");
      coldEmail(client.getEmail());
      client.setIsContacted(true);
      scraperRepository.save(client);
      try {
        long delay = 5000 + (long) (Math.random() * 25000); // random between 5s and 30s
        Thread.sleep(delay);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
