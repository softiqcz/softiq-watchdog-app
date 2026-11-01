package cz.softiq.tests.clients.csssro;

import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.service.automation.csssro.service.sync.ICsssroService;
import cz.softiq.tests.base.MainBrowserTestClass;
import org.testng.annotations.*;

public class EstateOfferSync extends MainBrowserTestClass implements ICsssroService {

  @Test
  public void syncClient() {
    successMessages.add(runEstateSync());
  }

  private String runEstateSync() {
    var reportBuilder = runAutomationJob();
    if (reportBuilder.getErrorMessage() != null) {
      throw new SoftiqException(reportBuilder.getErrorMessage());
    }
    return "<p>css-sro sync OK</p>";
  }
}
