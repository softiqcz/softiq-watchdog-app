package cz.softiq.tests.watchdog;

import static com.codeborne.selenide.Selenide.sleep;

import cz.softiq.service.watchdog.entity.WatchDogMembershipEntity;
import cz.softiq.service.watchdog.entity.WatchDogOldClientEntity;
import cz.softiq.tests.base.MainApiTestClass;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class FeedbackRequestEmail extends MainApiTestClass {
  private static final Logger log = LoggerFactory.getLogger(FeedbackRequestEmail.class);
  private static final long SEND_DELAY_MS = 5000;

  @Test
  public void sendFeedbackRequestEmail() {
    Map<String, String> recipients = new LinkedHashMap<>();

    for (WatchDogOldClientEntity oldClient : getOldClients()) {
      addRecipient(recipients, oldClient.getEmail());
    }

    for (WatchDogMembershipEntity activeMembership : getActiveWatchDogMemberships()) {
      if (activeMembership.getClient() != null) {
        addRecipient(recipients, activeMembership.getClient().getEmail());
      }
    }

    log.info("Sending feedback request to {} unique recipients", recipients.size());
    for (String email : recipients.values()) {
      log.info("Sending feedback request to: {}", email);
      feedbackRequestEmail(email);
      sleep(SEND_DELAY_MS);
    }
  }

  private void addRecipient(Map<String, String> recipients, String email) {
    if (email == null || email.isBlank()) {
      return;
    }

    String trimmedEmail = email.trim();
    recipients.putIfAbsent(trimmedEmail.toLowerCase(Locale.ROOT), trimmedEmail);
  }
}
