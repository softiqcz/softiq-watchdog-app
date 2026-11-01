package cz.softiq.tests.softiq;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;

import com.codeborne.selenide.ClickOptions;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.ex.ElementNotFound;
import com.codeborne.selenide.ex.ElementShould;
import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.core.helper.ReportBuilder;
import cz.softiq.tests.base.MainBrowserTestClass;
import java.time.Duration;
import java.util.UUID;
import org.testng.annotations.Test;

public class SoftiqGuiTest extends MainBrowserTestClass {
  private String globalDescription;

  @Test
  public void fullUserFlowTest() {
    executeWithFailureMessage(this::softiqE2E, "Softiq GUI Test FAILED");
  }

  private ReportBuilder runSoftiqTest() {
    String fakeEmail = "test-" + UUID.randomUUID().toString().substring(0, 8) + "@seznam.cz";
    String password = "TestPassword123!";
    ReportBuilder reportMessage = ReportBuilder.builder().build();

    executeInTryCatch(
        () -> {
          open("https://www.softiq.cz");
          WebDriverRunner.getWebDriver().manage().window().maximize();
          if ($x("//button[text()='Přijmout vše']").is(visible)) {
            $x("//button[text()='Přijmout vše']").click();
          }
        },
        "Navigate to softiq.cz");

    executeInTryCatch(
        () -> {
          $x("//button[text()='Registrace']").shouldBe(visible).click();
          $x("//input[@name='email']").shouldBe(visible).setValue(fakeEmail);
          $x("//input[@name='password']").setValue(password);
          $x("//input[@name='password-repeat']").setValue(password);
          $x("//button[text()='Registrovat se']").click();
          $x("//input[@id='termsAgreement']").shouldBe(visible).setSelected(true); // Agree to Terms
          $x("//button[text()='Pokračovat']").shouldBe(enabled).click();
        },
        "Register with fake email + select FREE membership");

    executeInTryCatch(
        () -> {
          $x("//button[text()='Profil']").shouldBe(visible, Duration.ofSeconds(10));
          $x("//li[@data-type='success' and .//*[text()='Registrace úspěšná']]").shouldBe(visible);
        },
        "Verify we are logged in");

    executeInTryCatch(
        () -> {
          $x("//button[text()='Zobrazit nebo vytvořit hlídacího psa']").shouldBe(visible).click();

          $x("//p[text()='Tipy k hlídacímu psovi']").shouldBe(visible);
          $x("//button[text()='Přidat hlídacího psa']").click();
          var dialog =
              $x("(//h2[text()='Vytvořit nového hlídacího psa']/ancestor::div[@role='dialog'])[last()]")
                  .should(appear);
          dialog.$x(".//button[span[text()='Typ nabídky']]").click();
          $x("//div[@role='option'][span[text()='Prodej']]").click(ClickOptions.usingJavaScript());
          dialog.$x(".//button[span[text()='Typ nemovitosti']]").click();
          $x("//div[@data-value='Byty']").click(ClickOptions.usingJavaScript());
          dialog.$x(".//input[@placeholder='Zadejte město']").setValue("Olomouc");

          dialog.$x(".//button[text()='Pokračovat']").click();
          dialog.$x(".//button[text()='Uložit']").click();

          $x("//li[@data-type='success' and .//*[text()='Hlídací pes vytvořen, brzy se můžete těšit na výsledky.']]")
              .should(exist);
        },
        "Create a new watchdog");

    executeInTryCatch(
        () -> {
          $x("//button[text()='Zvýšit členství']").shouldBe(visible).click();
          var dialogMembership =
              $x("(//h2[text()='Změnit členství']/ancestor::div[@role='dialog'])[last()]")
                  .should(appear);
          dialogMembership.$x(".//button[span[text()='Vyberte si nové členství']]").click();
          $x("//div[@role='option'][span[text()='Profesionální']]")
              .click(ClickOptions.usingJavaScript());
          $x("(//label[contains(., 'obchodními podmínkami')]//input[@type='checkbox'])[last()]")
              .setSelected(true);
          $x("(//label[contains(., 'opakovanou platbou')]//input[@type='checkbox'])[last()]")
              .setSelected(true);
          dialogMembership.$x(".//button[text()='Zrušit změny']").click();
        },
        "Upgrade to Professional membership");

    executeInTryCatch(
        () -> {
          $x("//button[text()='Profil']").click();
          $x("//a[text()='Smazat účet']").should(appear).click();
          var dialogDelete =
              $x("(//h2[text()='Smazat účet']/ancestor::div[@role='dialog'])[last()]")
                  .should(appear);
          dialogDelete.$x(".//button[text()='Uložit změny']").click();
          $x("//li[@data-type='success' and .//*[text()='Účet úspěšně smazán']]").should(exist);
          $x("//li[@data-type='success' and .//*[text()='Odhlášení proběhlo úspěšně']]")
              .should(visible);
        },
        "Delete account");
    return reportMessage;
  }

  private void executeInTryCatch(Runnable runnable, String description) {
    globalDescription = description;
    runnable.run();
  }

  private String softiqE2E() {
    try {
      var reportBuilder = runSoftiqTest();
      if (reportBuilder.getErrorMessage() != null) {
        throw new SoftiqException(reportBuilder.getErrorMessage());
      } else {
        return "<p>softiq.cz test OK</p>";
      }
    } catch (Exception | ElementNotFound | ElementShould e) {
      throw new SoftiqException(
          String.format(
              "<p style=\"color: red; font-weight: bold;\">%s FAILED<br><span>%s</span></p>",
              globalDescription, e.getMessage()));
    }
  }
}
