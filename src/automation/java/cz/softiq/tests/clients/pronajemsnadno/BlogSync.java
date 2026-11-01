package cz.softiq.tests.clients.pronajemsnadno;

import cz.softiq.service.automation.pronajemsnadno.IPronajemsnadnoService;
import cz.softiq.tests.base.MainBrowserTestClass;
import org.testng.annotations.Test;

public class BlogSync extends MainBrowserTestClass implements IPronajemsnadnoService {

  @Test
  public void runBlogSync() {
    successMessages.add(setLinkedInSearch());
  }
}
