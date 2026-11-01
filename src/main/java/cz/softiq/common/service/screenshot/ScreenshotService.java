package cz.softiq.common.service.screenshot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class ScreenshotService {
  private final ScreenshotComponent component;

  String getScreenshotMessage() {
    return component.getScreenshotMessage();
  }

  void cleanupOldScreenshots() {
    component.cleanupOldScreenshots();
  }

  String getScreenshotDir() {
    return component.getScreenshotDir();
  }
}
