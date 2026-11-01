package cz.softiq.service.automation.scraper.web;

import static com.codeborne.selenide.Selenide.*;

import com.codeborne.selenide.SelenideElement;
import cz.softiq.common.base.BaseBrowserClass;
import cz.softiq.service.automation.scraper.entity.ColdCallsEntity;
import cz.softiq.service.automation.scraper.service.IScraperService;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemaxWeb extends BaseBrowserClass implements IScraperService {

  private static final int TOTAL_PAGES = 165;
  private static final int SAVE_EVERY = 5;

  public RemaxWeb openWeb() {
    open("https://www.remax-czech.cz/reality/makleri/");
    sleep(5000);
    scrapeAndSave();
    return page(this);
  }

  private void scrapeAndSave() {
    List<ColdCallsEntity> batch = new ArrayList<>();

    for (int i = 1; i <= TOTAL_PAGES; i++) {
      var clients =
          $$x("//li[@class='row-list__item']//h3[contains(normalize-space(text()), 'Makléř')]");
      for (SelenideElement client : clients) {
        String email = client.$x(".//..//p//a[contains(@href, 'mailto')]").getText();
        String name = client.$x(".//..//h2/span").getText();
        batch.add(new ColdCallsEntity(name, email, "REMAX"));
      }

      if (i % SAVE_EVERY == 0) {
        log.info("Saving batch at page {}...", i);
        saveClients(batch);
        batch.clear();
      }

      if (i != TOTAL_PAGES) {
        $x("//nav[@aria-label='Page navigation']/ul/li/a[@title='další']").click();
        sleep(5000);
      }
      sleep(5000);
    }

    // save remaining pages that didn't fill a full batch
    if (!batch.isEmpty()) {
      log.info("Saving final batch of {} clients...", batch.size());
      saveClients(batch);
    }
  }

  private void saveClients(List<ColdCallsEntity> clients) {
    int skipped = 0;
    List<ColdCallsEntity> toSave = new ArrayList<>();

    for (ColdCallsEntity client : clients) {
      if (client.getEmail() == null || client.getEmail().isBlank()) {
        skipped++;
        continue;
      }
      if (findByEmail(client.getEmail()) == null) {
        toSave.add(client);
      } else {
        skipped++;
      }
    }

    saveAllClients(toSave);
    log.info("Saved: {}, Skipped: {}", toSave.size(), skipped);
  }
}
