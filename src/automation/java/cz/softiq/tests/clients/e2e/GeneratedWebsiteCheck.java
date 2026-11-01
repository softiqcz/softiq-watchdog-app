package cz.softiq.tests.clients.e2e;

import cz.softiq.service.automation.csssro.web.CssSroPage;
import cz.softiq.service.automation.envi24.web.Envi24Page;
import cz.softiq.service.automation.lars.Lars;
import cz.softiq.service.automation.softiq.web.SoftiqPage;
import cz.softiq.tests.base.MainBrowserTestClass;
import org.testng.annotations.Test;

public class GeneratedWebsiteCheck extends MainBrowserTestClass {

  @Test
  public void larsGuiTest() {
    successMessages.add(new Lars().checkAllPages());
  }

  @Test
  public void cssSroGuiTest() {
    successMessages.add(new CssSroPage().mainPageSectionsCheck("css-sro"));
  }

  //  @Test
  //  public void pronajemSnadnoGuiTest() {
  //    successMessages.add(new PronajemSnadnoPage().mainPageSectionsCheck());
  //  }

  @Test
  public void envi24GuiTest() {
    successMessages.add(new Envi24Page().mainPageSectionsCheck());
  }

  @Test
  public void softiqGuiTest() {
    successMessages.add(new SoftiqPage().mainPageSectionsCheck());
  }
}
