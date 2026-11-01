package cz.softiq.service.automation.helpdesk;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$x;
import static cz.softiq.service.automation.helpdesk.model.RoleEnum.SUPERADMIN;

import com.codeborne.selenide.Selenide;
import java.time.Duration;

public class Helpdesk {

  public String checkAllPages() {
    Selenide.open("http://svemar03.local:5173");
    $x("//h3[contains(text(),'Přihlášení do helpdesku')]").should(visible, Duration.ofSeconds(10));
    new HelpdeskVerifier().login(SUPERADMIN);
    return "<p>helpdesk svemar03.local:5173 GUI TEST FAILED</p>";
  }
}
