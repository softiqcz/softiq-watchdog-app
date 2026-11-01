package cz.softiq.service.automation.pronajemsnadno.web;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import cz.softiq.common.base.BaseBrowserClass;
import java.time.Duration;

public class PronajemSnadnoPage extends BaseBrowserClass {

  public String mainPageSectionsCheck() {
    openClientAndCheckFooter();
    $x("//h1[normalize-space(.)='Garantujeme Vám nájem vždy včas na Váš účet']").should(visible);
    checkHeader($("#sluzby"), "Naše služby s garantovaným pronájmem");
    checkHeader($("#jak-to-funguje"), "Jak to probíhá?");
    checkHeader($("#nabidky"), "Nabídka nemovitostí");
    return "<p>pronajemsnadno.cz GUI TEST OK</p>";
  }

  private void openClientAndCheckFooter() {
    Selenide.open("https://www.pronajemsnadno.cz");
    $x("//footer[.//a[contains(@href, 'weby.softiq.cz')]]")
        .should(Condition.exist, Duration.ofSeconds(15));
  }

  private static void checkHeader(SelenideElement section, String header) {
    section
        .$x(String.format(".//h2[normalize-space(.)='%s']", header))
        .scrollTo()
        .should(visible, Duration.ofSeconds(2));
  }
}
