package cz.softiq.service.automation.softiq.web;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$x;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import cz.softiq.common.base.BaseBrowserClass;
import java.time.Duration;

public class SoftiqPage extends BaseBrowserClass {

  public String mainPageSectionsCheck() {
    openClientAndCheckFooter();
    $x("//h1[text()='Správa realitního portfolia']").should(visible);
    $x("//p[text()='Hodnota, finance, dokumenty a provoz pod kontrolou.']").should(visible);
    return "<p>softiq.cz GUI TEST OK</p>";
  }

  private void openClientAndCheckFooter() {
    Selenide.open("https://www.softiq.cz");
    $x("//a[@href='https://softiq.cz']").should(Condition.exist, Duration.ofSeconds(15));
    $x("//span[contains(., 'FE') and contains(., 'v3.')]").should(Condition.exist);
    $x("//span[contains(., 'BE') and contains(., 'v3.')]").should(Condition.exist);
  }
}
