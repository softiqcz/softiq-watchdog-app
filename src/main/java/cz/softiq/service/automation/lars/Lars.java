package cz.softiq.service.automation.lars;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;

import com.codeborne.selenide.Selenide;
import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import java.time.Duration;

public class Lars implements IBeanProvider, IBaseService {

  public String checkAllPages() {
    Selenide.open("https://www.lahudkylars.cz");
    $x("//h1[normalize-space(.)='Lahůdky LARS.']").should(visible, Duration.ofSeconds(5));
    new LarsVerifier()
        .closeVacationModal()
        .closeParkingModal()
        .checkCenik()
        .checkProdejna()
        .checkNabidkaPrace()
        .checkKontakt()
        .checkNaseVyrobky();
    //        .checkObjednavka();
    return "<p>lahudkylars.cz GUI TEST OK</p>";
  }
}
