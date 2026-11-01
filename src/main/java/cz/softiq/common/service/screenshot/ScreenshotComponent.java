package cz.softiq.common.service.screenshot;

import com.codeborne.selenide.WebDriverRunner;
import cz.softiq.common.base.model.IBaseService;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class ScreenshotComponent implements IBaseService {
  private static final DateTimeFormatter SCREENSHOT_TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

  @Value("${softiq.test.screenshot.dir:build/screenshots}")
  protected String screenshotDir;

  @Value("${softiq.test.screenshot.base-url:http://svemar04.local:8081}")
  protected String screenshotBaseUrl;

  @Value("${softiq.test.screenshot.retention-days:2}")
  protected int screenshotRetentionDays;

  public String getScreenshotMessage() {
    try {
      Path screenshotFile = takeBrowserScreenshot(getScreenshotName());

      if (!Files.exists(screenshotFile) || Files.size(screenshotFile) == 0) {
        return String.format(
            "<p style=\"color: red; font-weight: bold;\">Screenshot not found. Expected: %s</p>",
            screenshotFile);
      }

      String screenshotUrl = getScreenshotUrl(screenshotFile.getFileName().toString());
      return String.format(
          """
          <div style="width: 100%%; max-width: 100%%; overflow: hidden;">
            <a href="%s" target="_blank" rel="noopener noreferrer" style="display: block; max-width: 100%%; overflow-wrap: anywhere; word-break: break-word;">%s</a>
              <a href="%s" target="_blank" rel="noopener noreferrer" style="display: block; width: 100%%; max-width: 100%%;">
                <img src="%s" alt="Test failure screenshot" width="100%%" style="display: block; width: 100%%; max-width: 100%%; height: auto; border: 0;" />
              </a>
          </div>
          """,
          screenshotUrl, screenshotUrl, screenshotUrl, screenshotUrl);
    } catch (Throwable e) {
      log.warn("Screenshot capture failed", e);
      return String.format(
          "<p style=\"color: red; font-weight: bold;\">Screenshot capture failed: %s</p>",
          e.getMessage());
    }
  }

  public void cleanupOldScreenshots() {
    Path screenshotsPath = getScreenshotPath();
    if (!Files.isDirectory(screenshotsPath)) {
      return;
    }

    Instant cutoff = Instant.now().minus(Duration.ofDays(screenshotRetentionDays));
    try (Stream<Path> files = Files.list(screenshotsPath)) {
      files
          .filter(Files::isRegularFile)
          .filter(path -> shouldDeleteScreenshotArtifact(path, cutoff))
          .forEach(this::deleteScreenshot);
    } catch (Exception ignored) {
      // Screenshot cleanup must not block test execution.
    }
  }

  public String getScreenshotDir() {
    String envScreenshotDir = System.getenv("SOFTIQ_TEST_SCREENSHOT_DIR");
    if (envScreenshotDir != null && !envScreenshotDir.isBlank()) {
      return envScreenshotDir;
    }

    String propertyScreenshotDir = System.getProperty("softiq.test.screenshot.dir");
    if (propertyScreenshotDir != null && !propertyScreenshotDir.isBlank()) {
      return propertyScreenshotDir;
    }

    return screenshotDir == null || screenshotDir.isBlank() ? "build/screenshots" : screenshotDir;
  }

  private Path takeBrowserScreenshot(String screenshotName) throws Exception {
    Path screenshotDir = getScreenshotPath();
    Files.createDirectories(screenshotDir);
    verifyScreenshotDirWritable(screenshotDir);

    Path screenshotFile = screenshotDir.resolve(screenshotName + ".png");
    try {
      WebDriver webDriver = getAvailableWebDriver();
      byte[] screenshotBytes = captureScreenshotBytes(webDriver);
      Files.write(screenshotFile, screenshotBytes);
      log.info("Stored browser failure screenshot at {}", screenshotFile.toAbsolutePath());
    } catch (Throwable screenshotFailure) {
      log.warn("Browser screenshot failed; storing diagnostic PNG instead", screenshotFailure);
      writeDiagnosticScreenshot(screenshotFile, screenshotFailure);
      log.info("Stored diagnostic failure screenshot at {}", screenshotFile.toAbsolutePath());
    }

    return screenshotFile;
  }

  private void verifyScreenshotDirWritable(Path screenshotDir) throws Exception {
    Path writeTest = Files.createTempFile(screenshotDir, ".write-test-", ".tmp");
    Files.deleteIfExists(writeTest);
  }

  private WebDriver getAvailableWebDriver() {
    if (!WebDriverRunner.hasWebDriverStarted()) {
      throw new IllegalStateException("WebDriver is not started");
    }

    WebDriver webDriver = WebDriverRunner.getWebDriver();
    if (webDriver == null) {
      throw new IllegalStateException("WebDriver is null");
    }

    webDriver.getWindowHandles();
    return webDriver;
  }

  private byte[] captureScreenshotBytes(WebDriver webDriver) {
    if (webDriver instanceof TakesScreenshot takesScreenshot) {
      return takesScreenshot.getScreenshotAs(OutputType.BYTES);
    }

    if (webDriver instanceof JavascriptExecutor javascriptExecutor) {
      Object result = javascriptExecutor.executeScript("return document.documentElement.outerHTML");
      throw new IllegalStateException(
          "WebDriver does not support screenshots. Current page HTML length: "
              + String.valueOf(result).length());
    }

    throw new IllegalStateException(
        "WebDriver does not support screenshots: " + webDriver.getClass().getName());
  }

  private void writeDiagnosticScreenshot(Path screenshotFile, Throwable failure) throws Exception {
    BufferedImage image = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    try {
      graphics.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      graphics.setColor(new Color(252, 252, 250));
      graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
      graphics.setColor(new Color(148, 32, 32));
      graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
      graphics.drawString("Screenshot capture failed", 48, 76);

      graphics.setColor(new Color(30, 30, 30));
      graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 22));
      drawWrappedText(graphics, getScreenshotDiagnosticText(failure), 48, 132, 1080, 32);
    } finally {
      graphics.dispose();
    }

    ImageIO.write(image, "png", screenshotFile.toFile());
  }

  private String getScreenshotDiagnosticText(Throwable failure) {
    String message = failure.getMessage();
    return String.format(
        """
        A PNG was stored, but it is a diagnostic image because the browser screenshot could not be captured.

        Screenshot directory:
        %s

        Cause:
        %s: %s
        """,
        getScreenshotPath(), failure.getClass().getName(), message == null ? "" : message);
  }

  private void drawWrappedText(
      Graphics2D graphics, String text, int x, int y, int width, int lineHeight) {
    int currentY = y;
    for (String paragraph : text.split("\\R")) {
      String line = "";
      for (String word : paragraph.split(" ")) {
        String candidate = line.isBlank() ? word : line + " " + word;
        if (graphics.getFontMetrics().stringWidth(candidate) > width && !line.isBlank()) {
          graphics.drawString(line, x, currentY);
          currentY += lineHeight;
          line = word;
        } else {
          line = candidate;
        }
      }

      if (!line.isBlank()) {
        graphics.drawString(line, x, currentY);
      }
      currentY += lineHeight;
    }
  }

  private String getScreenshotName() {
    return String.format("test-failure-%s", LocalDateTime.now().format(SCREENSHOT_TIMESTAMP))
        .replaceAll("[^A-Za-z0-9._-]", "-");
  }

  private String getScreenshotUrl(String fileName) {
    String screenshotBaseUrl = getScreenshotBaseUrl();
    String baseUrl = screenshotBaseUrl.endsWith("/") ? screenshotBaseUrl : screenshotBaseUrl + "/";
    return baseUrl + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private boolean shouldDeleteScreenshotArtifact(Path path, Instant cutoff) {
    String fileName = path.getFileName().toString();
    if (fileName.endsWith(".html")) {
      return true;
    }

    return fileName.endsWith(".png") && isOlderThan(path, cutoff);
  }

  private boolean isOlderThan(Path path, Instant cutoff) {
    try {
      FileTime lastModifiedTime = Files.getLastModifiedTime(path);
      return lastModifiedTime.toInstant().isBefore(cutoff);
    } catch (Exception e) {
      return false;
    }
  }

  private void deleteScreenshot(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (Exception ignored) {
      // The file may disappear between listing and deletion.
    }
  }

  private Path getScreenshotPath() {
    return Path.of(getScreenshotDir()).toAbsolutePath().normalize();
  }

  private String getScreenshotBaseUrl() {
    return screenshotBaseUrl == null || screenshotBaseUrl.isBlank()
        ? "http://svemar04.local:8081"
        : screenshotBaseUrl;
  }
}
