package cz.softiq.service.automation.helpdesk;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Selenide.$$x;
import static com.codeborne.selenide.Selenide.$x;

import cz.softiq.service.automation.helpdesk.model.RoleEnum;

public class HelpdeskVerifier {

  public void login(RoleEnum role) {
    $x("//input[@name='email']").setValue("svejdam@icloud.com");
    $x("//button[@type='submit']").click();
    $x("//input[@name='password']").should(appear).setValue("softiq");
    $x("//button[@type='submit']").click();
    sectionHeaderCheck("Domovská stránka");
    var items = $$x("//a[@class='menu__item']");
    if (role.equals(RoleEnum.SUPERADMIN)) {
      items.shouldHave(size(8));
    } else if (role.equals(RoleEnum.ADMIN)) {
      items.shouldHave(size(6));
    } else if (role.equals(RoleEnum.MANAGER)) {
      items.shouldHave(size(4));
    } else if (role.equals(RoleEnum.USER)) {
      items.shouldHave(size(2));
    }
  }

  private void sectionHeaderCheck(String header) {
    $x("//h1/span[text()='" + header + "']").should(appear);
  }

  public void createTicket() {
    $x("//a[@class='menu__item']/.//span[text()='Založit nový požadavek']").click();
    sectionHeaderCheck("Nový požadavek");
    emailWhisperer("requestor");
    selector("category");
    $x("//input[@name='issueHeader']").setValue("Test ticket header");
    $x("//input[@name='issueDescription']")
        .setValue("Test ticket description. this one is longer. like lorem ipsum idk.");
    emailWhisperer("coRequestor");
    $x("//button[@type='submit']").click();
    sectionHeaderCheck("Mé požadavky");
    openTicketDetail();
  }

  private void openTicketDetail() {
    $x("//tr/td").click();
    $x("//div[@id='ticket-detail']").should(appear);
  }

  private void selector(String name) {
    var element = $x("//input[@name='" + name + "']");
    element.click();
    $x("//button[text()='SAP']").click();
    element.shouldHave(value("SAP"));
  }

  private void emailWhisperer(String name) {
    var requestorField = $x("//input[@name='" + name + "']");
    requestorField.setValue("nussli");
    var suggestedUserElement = $x("(//ul/li[contains(text(),'nussli')])[2]");
    suggestedUserElement.should(appear);
    var suggestedUser = suggestedUserElement.getText();
    suggestedUserElement.click();
    requestorField.shouldHave(value(suggestedUser));
  }
}
