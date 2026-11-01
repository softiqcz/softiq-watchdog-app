package cz.softiq.tests.scraper;

import cz.softiq.service.automation.scraper.web.RemaxWeb;
import cz.softiq.tests.base.MainBrowserTestClass;
import org.testng.annotations.Test;

public class ScrapeNewClients extends MainBrowserTestClass {

  @Test
  public void scrape() {
    new RemaxWeb().openWeb();
  }
}
