package cz.softiq.tests.clients.pronajemsnadno;

import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.service.automation.pronajemsnadno.IPronajemsnadnoService;
import cz.softiq.tests.base.MainBrowserTestClass;
import org.testng.annotations.Test;

public class EstateOffersSync extends MainBrowserTestClass implements IPronajemsnadnoService {

  @Test
  public void syncClient() {
    successMessages.add(runEstateSync());
  }

  private String runEstateSync() {
    var reportBuilder = runAutomationJob();
    if (reportBuilder.getErrorMessage() != null) {
      throw new SoftiqException(reportBuilder.getErrorMessage());
    }
    return "<p>pronajemsnadno sync OK</p>";
  }
}
