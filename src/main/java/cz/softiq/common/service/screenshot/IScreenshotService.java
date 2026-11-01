package cz.softiq.common.service.screenshot;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;

public interface IScreenshotService extends IBeanProvider, IBaseService {

  default String getScreenshotMessage() {
    return getScreenshotService().getScreenshotMessage();
  }

  default void cleanupOldScreenshots() {
    getScreenshotService().cleanupOldScreenshots();
  }

  default String getScreenshotDir() {
    return getScreenshotService().getScreenshotDir();
  }

  private ScreenshotService getScreenshotService() {
    return getBean(ScreenshotService.class);
  }
}
