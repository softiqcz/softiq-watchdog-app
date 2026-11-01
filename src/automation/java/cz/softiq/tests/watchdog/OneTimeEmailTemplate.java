package cz.softiq.tests.watchdog;

import cz.softiq.tests.base.MainApiTestClass;
import org.testng.annotations.Test;

public class OneTimeEmailTemplate extends MainApiTestClass {

  @Test
  public void sendEmail() {
    sendMonthlyPortfolioSummaries();
  }
}
