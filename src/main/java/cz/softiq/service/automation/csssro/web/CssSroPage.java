package cz.softiq.service.automation.csssro.web;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import cz.softiq.common.base.BaseBrowserClass;
import java.time.Duration;

public class CssSroPage extends BaseBrowserClass {

  public String mainPageSectionsCheck(String clientName) {
    openClientAndCheckFooter(clientName);
    $x("//h2[contains(normalize-space(.), 'Prodejte s jistotou, kupujte s nadšením a starosti nechejte na mně.')]")
        .should(visible);
    servicesSection($("#sluzby"), "Mé služby");
    portfolioSection($("#nemovitosti"), "Nemovitosti");
    flipSection($("#realizace"), "Realizace");
    aboutSection($("#omne"), "O mně");
    reviewSection($("#recenze"), "Recenze");
    contactSection($("#kontakt"), "Kontakt");
    footerSection();
    return "<p>" + clientName + ".cz GUI TEST OK</p>";
  }

  private void openClientAndCheckFooter(String clientName) {
    Selenide.open(String.format("https://%s.cz", clientName));
    $x("//footer[@id='footer']//a[contains(@href, 'weby.softiq.cz')]")
        .should(Condition.exist, Duration.ofSeconds(15));
    SelenideElement acceptCookies = $x("//button[normalize-space(.)='Přijmout vše']");
    if (acceptCookies.isDisplayed()) {
      acceptCookies.click();
    }
  }

  private static void servicesSection(SelenideElement section, String header) {
    checkHeader(section, header);
    section.$x(".//h3[contains(text(),'Pronájem nemovitostí')]").should(visible);
    section
        .$x(".//p[contains(text(),'Pomáhám majitelům nemovitostí najít spolehlivé nájemníky.')]")
        .should(visible);
  }

  private static void portfolioSection(SelenideElement section, String header) {
    checkHeader(section, header);
    section.$x(".//button[@role='tab' and normalize-space(.)='Aktivní']").should(visible);
    section
        .$$x(".//a[starts-with(@aria-label, 'Více informací:')]")
        .findBy(visible)
        .should(visible);
    SelenideElement completedTab =
        section.$x(".//button[@role='tab' and normalize-space(.)='Zrealizováno']");
    completedTab.should(visible).click();
    completedTab.should(Condition.attribute("aria-selected", "true"));
  }

  private static void flipSection(SelenideElement section, String header) {
    checkHeader(section, header);
    section.$x(".//p[text()='Provedené kompletní rekonstrukce bytů']").should(visible);
  }

  private static void aboutSection(SelenideElement section, String header) {
    checkHeader(section, header);
    section.$x(".//p[contains(text(),'Jmenuji se Natálie Skřičková')]").should(visible);
    section.$x(".//li/strong[text()='Profesní zkušenost']").should(visible);
  }

  private static void reviewSection(SelenideElement section, String header) {
    checkHeader(section, header);
    section.$x(".//p[text()='Co o mně napsali mí klienti']").should(visible);

    section.$x(".//button[@aria-label='Další recenze']").should(visible).click();
  }

  private static void contactSection(SelenideElement section, String header) {
    checkHeader(section, header);
    section.$x(".//button[normalize-space(.)='Odhad nemovitosti zdarma']").should(visible).click();
    SelenideElement form = $x("//form[.//input[@placeholder='Vaše jméno']]").should(visible);
    form.$x(".//input[@placeholder='Vaše jméno']").setValue("Test Name");
    form.$x(".//input[@placeholder='Váš email']").setValue("test@softiq.cz");
    form.$x(".//button[normalize-space(.)='Odeslat zprávu']").scrollTo().should(visible);
  }

  private static void footerSection() {
    var section = $x("//footer[@id='footer']").should(visible);
    section.$x(".//h3[text()='Česká stavebně správní s.r.o.']").should(visible);
    section.$x(".//a[contains(@href, 'weby.softiq.cz')]").should(visible);
  }

  private static void checkHeader(SelenideElement section, String header) {
    var headerElement = section.$x(String.format(".//h2[normalize-space(.)='%s']", header));
    Selenide.executeJavaScript("arguments[0].scrollIntoView({block: 'center'});", headerElement);
    headerElement.should(visible, Duration.ofSeconds(2));
  }
}
