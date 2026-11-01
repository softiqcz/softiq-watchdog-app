package cz.softiq.service.automation.pronajemsnadno.pages;

import static com.codeborne.selenide.Selenide.*;
import static io.restassured.RestAssured.given;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.core.email.CoreLogEntity;
import cz.softiq.core.service.ICoreSoftiqService;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LinkedInBlogPage implements IBeanProvider, ICoreSoftiqService, IBaseService {

  public void doSearch(String token, String username, String url) {
    String linkedInUrl = "https://www.linkedin.com/in/marcel-dlouhý-42b5a583/recent-activity/all/";
    open(linkedInUrl);
    var loginButton = $x("//button[@type='submit' and contains(text(),'Sign in')]");

    if (!loginButton.isDisplayed()) {
      var loginForm = $x("//p[contains(text(),'Already on Linkedin?')]/button[text()=' Sign in ']");
      // maybe is already logged in
      if (loginForm.exists()) {
        loginForm.click();
      }
    }
    if (loginButton.isDisplayed()) {
      $x("//input[@name='session_key']").setValue("svejdam@icloud.com");
      $x("//input[@name='session_password']").setValue("softiq123");
      loginButton.click();
      loginButton.should(Condition.disappear, Duration.ofSeconds(30));
    }
    open(linkedInUrl);
    var posts =
        $$x(
            "//div[text()='Loaded 20 Posts posts' or text()='20 Příspěvky příspěvků načteno']/../ul/li");
    posts.get(0).should(Condition.visible, Duration.ofSeconds(120));
    for (int i = 0; i < 4; i++) {
      executeJavaScript("arguments[0].scrollIntoView({block: 'center'});", posts.get(i));
      String sneakPeak =
          posts
              .get(i)
              .$x(".//div[contains(@class,'commentary')]//span[@dir='ltr']")
              .getAttribute("outerHTML");
      String header = extractFirstCommentContent(sneakPeak);
      SelenideElement postImg =
          posts.get(i).$x(".//img[@alt='Tento obrázek nemá žádný alternativní popisek']");
      String picUrl = null;

      if (postImg.exists()) {
        picUrl = postImg.getAttribute("src");
      }
      Map<String, Object> requestBody =
          createRequestBody(header, sneakPeak, picUrl, token, username);
      saveNewBlog(requestBody, token, url);
    }
  }

  private static String extractFirstCommentContent(String input) {
    Pattern pattern = Pattern.compile("<!---->(.*?)<!---->");
    Matcher matcher = pattern.matcher(input);

    if (matcher.find()) {
      return matcher.group(1).trim();
    }
    return null;
  }

  private void saveNewBlog(Map<String, Object> requestBody, String token, String url) {
    try {
      Response response =
          given()
              .contentType(ContentType.JSON)
              .accept(ContentType.JSON)
              .header("Authorization", "Bearer " + token)
              .body(requestBody)
              .when()
              .post(url + "/api/v2/admin/blog");

      if (response.statusCode() == 200 || response.statusCode() == 201) {
        log.info(response.getBody().asString());
      } else {
        String error =
            String.format(
                "Blog save failed. Response code: %s, body: %s",
                response.statusCode(), response.getBody().asString());

        CoreLogEntity logEntity =
            new CoreLogEntity(null, LocalDate.now(), LocalDateTime.now(), error);
        saveLog(logEntity);
        log.error(error);
        throw new SoftiqException(error);
      }

    } catch (Exception e) {
      throw new SoftiqException(e.getMessage());
    }
  }

  private Map<String, Object> createRequestBody(
      String header, String sneakPeak, String picUrl, String token, String username) {
    Map<String, Object> blogData = new HashMap<>();
    blogData.put("header", header);
    blogData.put("sneakPeak", sneakPeak);
    if (picUrl != null) {
      blogData.put("picUrl", picUrl);
    }
    return Map.of("email", username, "token", token, "data", blogData);
  }
}
