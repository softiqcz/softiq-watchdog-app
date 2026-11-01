package cz.softiq.service.automation.csssro.service.sync;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.core.helper.ReportBuilder;
import cz.softiq.service.automation.watchdogSearch.service.ISearchService;

public interface ICsssroService extends IBeanProvider, IBaseService, ISearchService {

  default ReportBuilder runAutomationJob() {
    return syncCssSro();
  }
}
