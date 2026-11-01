package cz.softiq.tests.softiq;

import static cz.softiq.service.softiq.model.constants.MessageConstants.*;
import static org.testng.Assert.*;

import cz.softiq.core.model.builder.GenericResponse;
import cz.softiq.service.softiq.model.builder.BlogPreview;
import cz.softiq.tests.base.MainSoftiqTestClass;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testng.annotations.Test;

public class UnsecuredControllerTest extends MainSoftiqTestClass {

  @Test
  public void versionTest() {
    executeWithFailureMessage(this::version, "Version Test FAILED");
  }

  private String version() {
    ResponseEntity<GenericResponse<Map<String, String>>> response =
        getEndpoint("/api/v2/public/version");
    assertSuccess(response);
    String version = response.getBody().getData().get("version");
    assertTrue(version.startsWith("v"), "Version should start with v, but was: " + version);
    return "<p>version passed</p>";
  }

  @Test
  public void blogTest() {
    executeWithFailureMessage(this::blog, "Blog Test FAILED");
  }

  private String blog() {
    ResponseEntity<GenericResponse<Map<String, Object>>> response =
        getEndpoint("/api/v2/public/blog/get");
    assertSuccess(response);
    Map<String, Object> data = response.getBody().getData();
    BlogPreview blog =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .findAndRegisterModules()
            .convertValue(data.get("blog"), BlogPreview.class);
    assertNotNull(blog);
    assertTrue(blog.blogPosts().size() > 1, "Should have more than 1 blog post");
    return "<p>blog passed</p>";
  }

  @Test
  public void authLoginInvalidTest() {
    executeWithFailureMessage(this::authLoginInvalid, "Auth Login Invalid Test FAILED");
  }

  private String authLoginInvalid() {
    Map<String, String> request = Map.of("email", "invalid@softiq.cz", "password", "wrongpass");
    ResponseEntity<GenericResponse<Map<String, Object>>> response =
        postEndpoint("/api/v2/public/auth/login", request);
    assertEquals(response.getStatusCode(), HttpStatus.FORBIDDEN);
    assertNotNull(response.getBody());
    assertFalse(response.getBody().isSuccess());
    assertEquals(response.getBody().getMessage(), INVALID_CREDENTIALS);
    return "<p>authLoginInvalid passed</p>";
  }

  @Test
  public void registerDuplicateTest() {
    executeWithFailureMessage(this::registerDuplicate, "Register Duplicate Test FAILED");
  }

  private String registerDuplicate() {
    Map<String, Object> request = Map.of("email", adminEmail, "password", "password123");
    ResponseEntity<GenericResponse<Map<String, Object>>> response =
        postEndpoint("/api/v2/public/register/user", request);
    assertEquals(response.getStatusCode(), HttpStatus.CONFLICT);
    assertNotNull(response.getBody());
    assertFalse(response.getBody().isSuccess());
    assertEquals(response.getBody().getMessage(), DUPLICATE_USER);
    return "<p>registerDuplicate passed</p>";
  }

  @Test
  public void contactTest() {
    executeWithFailureMessage(this::contact, "Contact Test FAILED");
  }

  private String contact() {
    Map<String, Object> request =
        Map.of(
            "email", "test@example.com",
            "name", "Test User",
            "message", "Hello Test",
            "ownerEmail", "info@softiq.cz");
    ResponseEntity<GenericResponse<Map<String, Object>>> response =
        postEndpoint("/api/v2/public/contact", request);
    assertSuccess(response, CONTACT_OK);
    return "<p>contact passed</p>";
  }
}
