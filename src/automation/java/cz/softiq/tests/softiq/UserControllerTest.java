package cz.softiq.tests.softiq;

import static cz.softiq.service.softiq.model.constants.MessageConstants.*;

import cz.softiq.core.model.builder.GenericResponse;
import cz.softiq.tests.base.MainSoftiqTestClass;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.testng.annotations.Test;

public class UserControllerTest extends MainSoftiqTestClass {

  @Test
  public void getUserInfoTest() {
    executeWithFailureMessage(this::getUserInfo, "Get User Info Test FAILED");
  }

  private String getUserInfo() {
    ResponseEntity<GenericResponse<Map<String, Object>>> response =
        postEndpoint("/api/v2/user/profile/info", payload);
    assertNotEmptyData(response);
    return "<p>getUserInfo passed</p>";
  }

  @Test
  public void fetchWatchdogSettingsTest() {
    executeWithFailureMessage(this::fetchWatchdogSettings, "Fetch Watchdog Settings Test FAILED");
  }

  private String fetchWatchdogSettings() {
    ResponseEntity<GenericResponse<Object>> response =
        postEndpoint("/api/v2/user/watchdog/all", payload);
    assertNotEmptyData(response);
    return "<p>fetchWatchdogSettings passed</p>";
  }

  @Test
  public void fetchLatestResultsTest() {
    executeWithFailureMessage(this::fetchLatestResults, "Fetch Latest Results Test FAILED");
  }

  private String fetchLatestResults() {
    ResponseEntity<GenericResponse<Object>> response =
        postEndpoint("/api/v2/user/watchdog/results/latest", payload);
    assertNotEmptyData(response);
    return "<p>fetchWatchdogSettings passed</p>";
  }

  @Test
  public void fetchUserEstatesTest() {
    executeWithFailureMessage(this::fetchUserEstates, "Fetch User Estates Test FAILED");
  }

  private String fetchUserEstates() {
    ResponseEntity<GenericResponse<Object>> response =
        postEndpoint("/api/v2/user/estate/all", payload);
    assertNotEmptyData(response);
    return "<p>fetchUserEstates passed</p>";
  }

  @Test
  public void requestNewFeatureTest() {
    executeWithFailureMessage(this::requestNewFeature, "Request New Feature Test FAILED");
  }

  private String requestNewFeature() {
    Map<String, Object> data = Map.of("type", "NEW_FEATURE", "message", "Test feature request");
    ResponseEntity<GenericResponse<Object>> response =
        postEndpoint("/api/v2/user/utils/support/request", createDataRequest(data));
    assertSuccess(response, EDIT_SUCCESS_SIMPLE);
    return "<p>requestNewFeature passed</p>";
  }

  @Test
  public void saveProfileTest() {
    executeWithFailureMessage(this::saveProfile, "Save Profile Test FAILED");
  }

  private String saveProfile() {
    Map<String, Object> request =
        new HashMap<>(
            Map.of(
                "isActive", true,
                "address", "Test Address",
                "idNumber", "12345678",
                "subject", "Test Subject",
                "email", adminEmail));
    request.put("token", adminToken);

    ResponseEntity<GenericResponse<Map<String, Object>>> response =
        postEndpoint("/api/v2/user/profile/save", request);
    assertSuccess(response, BILLING_INFO_SAVED);
    return "<p>saveProfile passed</p>";
  }

  @Test
  public void fetchResultsByOffsetTest() {
    executeWithFailureMessage(this::fetchResultsByOffset, "Fetch Results By Offset Test FAILED");
  }

  private String fetchResultsByOffset() {
    Map<String, Object> data = Map.of("offset", 0);
    ResponseEntity<GenericResponse<Object>> response =
        postEndpoint("/api/v2/user/watchdog/results/byOffset", createDataRequest(data));
    assertNotEmptyData(response);
    return "<p>fetchResultsByOffset passed</p>";
  }

  @Test
  public void fetchWatchdogChildrenTest() {
    executeWithFailureMessage(this::fetchWatchdogChildren, "Fetch Watchdog Children Test FAILED");
  }

  private String fetchWatchdogChildren() {
    Map<String, Object> data = Map.of("watchDogId", 588);
    ResponseEntity<GenericResponse<Object>> response =
        postEndpoint("/api/v2/user/watchdog/results/children", createDataRequest(data));
    assertNotEmptyData(response);
    return "<p>fetchResultsByOffset passed</p>";
  }
}
