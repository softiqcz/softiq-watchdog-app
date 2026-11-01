package cz.softiq.service.automation.envi24.web;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import cz.softiq.common.base.BaseBrowserClass;
import java.time.Duration;

public class Envi24Page extends BaseBrowserClass {

  public String mainPageSectionsCheck() {
    openClientAndCheckFooter();
    $x("//h1[contains(.,'Chráníme přírodu')]").should(visible);
    $x("//*[text()='Ekologické poradenství pro firmy']").should(visible);
    checkHeader($("#sluzby"), "Kompletní ekologický servis");
    checkHeader($("#spoluprace"), "Jak funguje naše spolupráce?");
    checkHeader($("#skoleni"), "Odborná školení");
    checkHeader($("#kontakt"), "Kontakt");
    return "<p>envi24.cz GUI TEST OK</p>";
  }

  private void openClientAndCheckFooter() {
    Selenide.open("https://www.envi24.cz");
    $(".envi-footer").shouldHave(Condition.text("ENVI – Help s.r.o."), Duration.ofSeconds(15));
  }

  private static void checkHeader(SelenideElement section, String header) {
    section
        .$x(String.format(".//h2[text()='%s']", header))
        .scrollTo()
        .should(visible, Duration.ofSeconds(2));
  }
}
