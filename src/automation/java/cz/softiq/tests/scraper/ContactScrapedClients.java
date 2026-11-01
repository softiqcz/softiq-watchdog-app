package cz.softiq.tests.scraper;

import cz.softiq.service.automation.scraper.service.IScraperService;
import cz.softiq.tests.base.MainBrowserTestClass;
import org.testng.annotations.Test;

public class ContactScrapedClients extends MainBrowserTestClass implements IScraperService {

  @Test
  public void contact() {
    contactScraped(35);
  }
}
