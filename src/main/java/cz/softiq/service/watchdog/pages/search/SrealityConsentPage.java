package cz.softiq.service.watchdog.pages.search;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;
import static com.codeborne.selenide.Selenide.executeJavaScript;
import static com.codeborne.selenide.Selenide.sleep;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;

import com.codeborne.selenide.SelenideElement;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chromium.ChromiumDriver;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SrealityConsentPage {

  private static final String CONSENT_HOST = ".szn-cmp-dialog-container";
  private static final String ACCEPT_BUTTON_TEST_ID = "cw-button-agree-with-ads";

  public static void acceptCookies() {
    closePremiumBanner();

    SelenideElement shadowHost = $(CONSENT_HOST);
    for (int attempt = 0; attempt < 20; attempt++) {
      if (!shadowHost.exists() || !shadowHost.isDisplayed()) {
        sleep(250);
        continue;
      }

      if (clickInOpenShadowRoot(shadowHost) || clickInClosedShadowRoot()) {
        log.info("Accepted cookies on sreality.cz");
        return;
      }
      sleep(250);
    }

    log.info("No actionable Sreality cookie dialog found");
  }

  public static void closePremiumBanner() {
    SelenideElement premiumBanner = $x("//div[@data-e2e='premium-is-here']");
    if (premiumBanner.exists() && premiumBanner.isDisplayed()) {
      premiumBanner.$x(".//button").click();
    }
  }

  private static boolean clickInOpenShadowRoot(SelenideElement shadowHost) {
    WebElement button =
        executeJavaScript(
            "const root = arguments[0].shadowRoot;"
                + "return root && root.querySelector('button[data-testid=\""
                + ACCEPT_BUTTON_TEST_ID
                + "\"]');",
            shadowHost);
    if (button == null) {
      return false;
    }
    button.click();
    return true;
  }

  private static boolean clickInClosedShadowRoot() {
    WebDriver driver = getWebDriver();
    if (!(driver instanceof ChromiumDriver chromiumDriver)) {
      log.warn("Cannot inspect Sreality closed shadow DOM with driver {}", driver.getClass());
      return false;
    }

    chromiumDriver.executeCdpCommand("DOM.enable", Map.of());
    Map<String, Object> document =
        chromiumDriver.executeCdpCommand(
            "DOM.getFlattenedDocument", Map.of("depth", -1, "pierce", true));

    Object nodesValue = document.get("nodes");
    if (!(nodesValue instanceof List<?> nodes)) {
      return false;
    }

    for (Object nodeValue : nodes) {
      if (!(nodeValue instanceof Map<?, ?> node) || !isAcceptButton(node)) {
        continue;
      }

      Object backendNodeId = node.get("backendNodeId");
      if (!(backendNodeId instanceof Number nodeId)) {
        continue;
      }

      Map<String, Object> resolved =
          chromiumDriver.executeCdpCommand(
              "DOM.resolveNode", Map.of("backendNodeId", nodeId.longValue()));
      if (!(resolved.get("object") instanceof Map<?, ?> object)
          || !(object.get("objectId") instanceof String objectId)) {
        continue;
      }

      chromiumDriver.executeCdpCommand(
          "Runtime.callFunctionOn",
          Map.of(
              "objectId",
              objectId,
              "functionDeclaration",
              "function () { this.click(); }",
              "returnByValue",
              true));
      return true;
    }
    return false;
  }

  private static boolean isAcceptButton(Map<?, ?> node) {
    if (!"BUTTON".equals(node.get("nodeName"))
        || !(node.get("attributes") instanceof List<?> attrs)) {
      return false;
    }

    for (int index = 0; index + 1 < attrs.size(); index += 2) {
      if ("data-testid".equals(attrs.get(index))
          && ACCEPT_BUTTON_TEST_ID.equals(attrs.get(index + 1))) {
        return true;
      }
    }
    return false;
  }
}
