package cz.softiq.tests.softiq;

import static cz.softiq.service.softiq.model.constants.MessageConstants.*;

import cz.softiq.core.model.builder.GenericResponse;
import cz.softiq.tests.base.MainSoftiqTestClass;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.testng.annotations.Test;

public class AdminControllerTest extends MainSoftiqTestClass {

  @Test
  public void getAllActiveUsersTest() {
    executeWithFailureMessage(this::getAllActiveUsers, "Get All Active Users Test FAILED");
  }

  private String getAllActiveUsers() {
    ResponseEntity<GenericResponse<Object>> response =
        postEndpoint("/api/v2/admin/users/active/all", payload);
    assertNotEmptyData(response);
    return "<p>getAllActiveUsers passed</p>";
  }

  @Test
  public void switchToUserTest() {
    executeWithFailureMessage(this::switchToUser, "Switch To User Test FAILED");
  }

  private String switchToUser() {
    Map<String, Object> data = Map.of("email", adminEmail);
    ResponseEntity<GenericResponse<Object>> response =
        postEndpoint("/api/v2/admin/users/switch", createDataRequest(data));
    assertSuccess(response, String.format(ADMIN_SWITCH_SUCCESS, adminEmail));
    return "<p>switchToUser passed</p>";
  }

  @Test
  public void getLogsTest() {
    executeWithFailureMessage(this::getLogs, "Get Logs Test FAILED");
  }

  private String getLogs() {
    ResponseEntity<GenericResponse<Object>> response =
        postEndpoint("/api/v2/admin/utils/logs/get", payload);
    assertSuccess(response, EDIT_SUCCESS_SIMPLE);
    return "<p>getLogs passed</p>";
  }

  @Test
  public void getReportsTest() {
    executeWithFailureMessage(this::getReports, "Get Reports Test FAILED");
  }

  private String getReports() {
    ResponseEntity<GenericResponse<Object>> response =
        postEndpoint("/api/v2/admin/utils/reports/get", payload);
    assertSuccess(response, EDIT_SUCCESS_SIMPLE);
    return "<p>getReports passed</p>";
  }

  @Test
  public void getTestReportsTest() {
    executeWithFailureMessage(this::getTestReports, "Get Test Reports Test FAILED");
  }

  private String getTestReports() {
    ResponseEntity<GenericResponse<Object>> response =
        postEndpoint("/api/v2/admin/utils/tests/reports/get", payload);
    assertSuccess(response, EDIT_SUCCESS_SIMPLE);
    return "<p>getTestReports passed</p>";
  }
}
