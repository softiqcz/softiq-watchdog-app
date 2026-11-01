package cz.softiq.tests.base;

import static org.testng.Assert.*;

import cz.softiq.core.model.builder.GenericResponse;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.testng.annotations.BeforeClass;

public abstract class MainSoftiqTestClass extends MainApiTestClass {

  @Value("${softiq.api.base-url}")
  private String apiBaseUrl;

  @Value("${softiq.api.test.email}")
  protected String adminEmail;

  @Value("${softiq.api.test.password}")
  private String apiTestPassword;

  protected String adminToken;
  protected Map<String, Object> payload;

  @BeforeClass
  public void setUp() {
    if (apiBaseUrl.isBlank() || adminEmail.isBlank() || apiTestPassword.isBlank()) {
      throw new IllegalStateException(
          "Set SOFTIQ_API_BASE_URL, SOFTIQ_API_TEST_EMAIL and SOFTIQ_API_TEST_PASSWORD before running the API suite");
    }
    Map<String, String> loginRequest = Map.of("email", adminEmail, "password", apiTestPassword);
    ResponseEntity<GenericResponse<Map<String, Object>>> loginResponse =
        postEndpoint("/api/v2/public/auth/login", loginRequest);

    assertEquals(loginResponse.getStatusCode(), HttpStatus.OK);
    adminToken = (String) loginResponse.getBody().getData().get("token");

    payload = createTokenRequest();
  }

  protected <T> ResponseEntity<GenericResponse<T>> postEndpoint(String path, Object body) {
    return callEndpoint(HttpMethod.POST, path, body);
  }

  protected <T> ResponseEntity<GenericResponse<T>> getEndpoint(String path) {
    return callEndpoint(HttpMethod.GET, path, null);
  }

  private <T> ResponseEntity<GenericResponse<T>> callEndpoint(
      HttpMethod method, String path, Object body) {
    var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(10_000);
    requestFactory.setReadTimeout(30_000);
    RestTemplate client = new RestTemplate(requestFactory);
    // Negative API checks assert the actual HTTP response, including 4xx bodies.
    client.setErrorHandler(
        new DefaultResponseErrorHandler() {
          @Override
          public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
            return false;
          }
        });
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
    if (adminToken != null) headers.setBearerAuth(adminToken);
    return client.exchange(
        apiBaseUrl.replaceAll("/+$", "") + path,
        method,
        new HttpEntity<>(body, headers),
        new ParameterizedTypeReference<GenericResponse<T>>() {});
  }

  protected Map<String, Object> createTokenRequest() {
    Map<String, Object> request = new HashMap<>();
    request.put("token", adminToken);
    request.put("email", adminEmail);
    return request;
  }

  protected Map<String, Object> createDataRequest(Map<String, Object> data) {
    Map<String, Object> request = new HashMap<>(payload);
    request.put("data", data);
    return request;
  }

  protected void assertSuccess(ResponseEntity<? extends GenericResponse<?>> response) {
    assertEquals(response.getStatusCode(), HttpStatus.OK);
    assertNotNull(response.getBody());
    assertTrue(response.getBody().isSuccess());
  }

  protected void assertSuccess(
      ResponseEntity<? extends GenericResponse<?>> response, String expectedMessage) {
    assertSuccess(response);
    assertEquals(response.getBody().getMessage(), expectedMessage);
  }

  protected void assertNotEmptyData(ResponseEntity<? extends GenericResponse<?>> response) {
    assertSuccess(response);

    Object data = response.getBody().getData();
    assertNotNull(data);

    if (data instanceof Collection<?> collection) {
      assertFalse(collection.isEmpty(), "Data is empty:");
    } else if (data instanceof Map<?, ?> map) {
      assertFalse(map.isEmpty(), "Data is empty:");
    } else if (data.getClass().isArray()) {
      assertTrue(Array.getLength(data) > 0);
    }
  }
}
