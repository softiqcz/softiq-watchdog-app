package cz.softiq.service.automation.lars;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import java.time.Duration;
import org.testng.Assert;

public class LarsVerifier {

  LarsVerifier closeVacationModal() {
    var cookieBanner = $x("//button[normalize-space(.)='Rozumím']");
    if (cookieBanner.isDisplayed()) {
      cookieBanner.click();
    }
    var vacationmodal = $x("//div[contains(@class,'confirmation-popup active')]");
    if (vacationmodal.is(visible)) {
      vacationmodal.$x(".//button[text()='Zavřít']").click();
      vacationmodal.should(disappear);
    }
    return page(this);
  }

  LarsVerifier closeParkingModal() {
    var parkingModal = $x("//div[contains(@class,'ui-card ui-dialog-surface')]");
    if (parkingModal.is(visible)) {
      $x("//button[normalize-space(.)='Kde parkovat?']").shouldBe(visible);
      parkingModal.$x(".//button[text()='Zavřít']").click();
      parkingModal.should(disappear);
    }
    return page(this);
  }

  LarsVerifier checkObjednavka() {
    switchToTab("/#objednavka");
    $x("//h1[text()='Objednávkový formulář']").should(visible, Duration.ofSeconds(5));
    $x("//input[@id='name']").setValue("Test test");
    $x("//input[@id='phone']").setValue("770770770");
    $x("//input[@id='email']").setValue("info@softiq.cz");
    $x("//input[@id='email_2']").setValue("info@softiq.cz");
    sleep(2000);
    $x("//input[@id='selectedDate']").click();
    sleep(2000);
    $x("//div[contains(@class,'flatpickr-calendar')]").should(appear, Duration.ofSeconds(5));
    $x("//span[@class='flatpickr-day ']").click();
    String selectedDate = $x("//input[@id='selectedDate']").getValue();
    Assert.assertTrue(
        selectedDate != null && !selectedDate.isEmpty(), "Selected date should not be empty");
    $x("//select[@id='selectedTime']")
        .should(Condition.appear, Duration.ofSeconds(5))
        .should(Condition.enabled, Duration.ofSeconds(5));
    $x("//select[@id='selectedTime']").click();
    $x("//select[@id='selectedTime']").selectOption(1);
    Selenide.executeJavaScript(
        "arguments[0].dispatchEvent(new Event('input'));", $x("//select[@id='selectedTime']"));
    $x("//select[@id='lahudky']").shouldBe(Condition.visible, Duration.ofSeconds(5)).click();
    $x("//select[@id='lahudky']").selectOption(1);
    $x("//select[@id='prichutSelect']")
        .should(Condition.appear, Duration.ofSeconds(5))
        .should(Condition.enabled, Duration.ofSeconds(5));
    $x("//select[@id='prichutSelect']").click();
    $x("//select[@id='prichutSelect']").selectOption(1);
    Selenide.executeJavaScript(
        "arguments[0].dispatchEvent(new Event('input'));", $x("//select[@id='prichutSelect']"));
    $x("//select[@id='countSelect']")
        .should(Condition.appear, Duration.ofSeconds(5))
        .scrollTo()
        .click();
    $x("//select[@id='countSelect']").selectOption(1);
    $x("//div[@id='confirmButton']").shouldNotHave(Condition.cssClass("disabled")).click();
    increaseItemCount();
    $x("//button[@id='send']").shouldNotBe(disabled).click();
    $x("//div[contains(@class,'confirmation-popup active')]")
        .should(visible, Duration.ofSeconds(10));
    return page(this);
  }

  LarsVerifier increaseItemCount() {
    var addedItemRow = $x("//table[@id='finalResults']/tbody/tr").should(appear);
    var priceColumn = addedItemRow.$$x(".//td").get(2);
    var priceBefore = priceColumn.getText();
    addedItemRow.$x(".//td/button[@onclick='plus(this)']").click();
    var priceAfter = priceColumn.getText();
    Assert.assertNotEquals(priceBefore, priceAfter, "Price was not changed!");
    return page(this);
  }

  LarsVerifier checkNaseVyrobky() {
    switchToTab("/#objednavka");
    $x("//main").should(visible, Duration.ofSeconds(5));
    $x("//a[contains(@aria-label, 'Košík') or normalize-space(.)='Košík']")
        .should(visible, Duration.ofSeconds(5));
    return page(this);
  }

  LarsVerifier checkCenik() {
    $x("//h3[normalize-space(.)='Ceník zašleme na vyžádání.']")
        .should(visible, Duration.ofSeconds(5));
    return page(this);
  }

  LarsVerifier checkProdejna() {
    switchToTab("/#prodejna");
    $x("//*[contains(normalize-space(.), 'Na Parcelách 71, Bořanovice, 250 65')]")
        .should(visible, Duration.ofSeconds(5));
    return page(this);
  }

  LarsVerifier checkNabidkaPrace() {
    switchToTab("/nabidka-prace");
    $x("//h2[normalize-space(.)='Nabízíme práci ve výrobně']")
        .should(visible, Duration.ofSeconds(5));
    return page(this);
  }

  LarsVerifier checkKontakt() {
    switchToTab("/kontakt");
    $x("//h2[normalize-space(.)='Napište nám']").should(visible, Duration.ofSeconds(5));
    $x("//form//button[normalize-space(.)='Odeslat zprávu']")
        .should(visible, Duration.ofSeconds(5));
    return page(this);
  }

  LarsVerifier switchToTab(String tabName) {
    $x(String.format("//a[@href=\"%s\"]", tabName)).should(visible).click();
    return page(this);
  }
}
