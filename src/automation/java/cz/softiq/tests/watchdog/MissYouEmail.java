package cz.softiq.tests.watchdog;

import cz.softiq.service.softiq.service.unsubscribe.IUnsubscribeService;
import cz.softiq.service.watchdog.entity.WatchDogOldClientEntity;
import cz.softiq.tests.base.MainApiTestClass;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;

@Slf4j
public class MissYouEmail extends MainApiTestClass implements IUnsubscribeService {

  @Test
  public void missYouEmail() {
    for (WatchDogOldClientEntity oldClient : getOldClients()) {
      var email = oldClient.getEmail();
      var note = oldClient.getNote();
      if (note == null || !note.contains("miss-you")) {
        //        log.info("sending email to: %s".formatted(email));
        //        missYouEmail(email);
        //        Selenide.sleep(5000);
      }
    }
  }
}
