package cz.softiq.service.automation.pronajemsnadno;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.service.automation.offerSync.service.rest.IRestApiService;
import cz.softiq.service.automation.pronajemsnadno.pages.LinkedInBlogPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class PronajemSnadnoSyncComponent implements IBaseService, IRestApiService {

  @Value("${spring.mail.username}")
  private String username;

  public String setLinkedInSearch() {
    String url = "https://pronajemsnadno.cz";
    String token = getToken(url);
    new LinkedInBlogPage().doSearch(token, username, url);
    return "<p>Pronajemsnadno blog sync OK</p>";
  }
}
