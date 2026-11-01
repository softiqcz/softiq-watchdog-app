package cz.softiq.tests.clients.helpdesk;

import cz.softiq.service.automation.helpdesk.Helpdesk;
import cz.softiq.tests.base.MainBrowserTestClass;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.Test;

public class HelpdeskLocalCheck extends MainBrowserTestClass {

  @Test
  public void helpdeskLocalCheck() {
    List<String> exceptionsTest = new ArrayList<>();
    exceptionsTest =
        executeWithExceptionHandling(
            new Helpdesk()::checkAllPages, exceptionsTest, successMessages);
    if (!exceptionsTest.isEmpty()) {
      String errorMessage =
          "<p style=\"color: red; font-weight: bold;\">helpdesk svemar03.local:5173 GUI TEST FAILED</h2>";
      exceptions.add(errorMessage);
      exceptions.add(String.join("", exceptionsTest));
    }
  }
}
