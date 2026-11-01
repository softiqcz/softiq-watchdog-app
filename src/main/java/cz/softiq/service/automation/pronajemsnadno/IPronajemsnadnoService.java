package cz.softiq.service.automation.pronajemsnadno;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.core.helper.ReportBuilder;
import cz.softiq.service.automation.watchdogSearch.service.ISearchService;

public interface IPronajemsnadnoService extends IBeanProvider, IBaseService, ISearchService {

  private PronajemSnadnoSyncService getLinkedInService() {
    return getBean(PronajemSnadnoSyncService.class);
  }

  default String setLinkedInSearch() {
    return getLinkedInService().setLinkedInSearch();
  }

  default ReportBuilder runAutomationJob() {
    return syncPronajemSnadno();
  }
}
