package com.automation.utils;

import com.automation.driver.DriverManager;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ScreenshotUtils — Captures browser screenshots for failure analysis and reporting.
 *
 * TWO CAPTURE MODES:
 * ───────────────────
 *  1. capture()        → Saves screenshot as a .png file on disk.
 *                        Used when you want a physical file to attach to bug reports
 *                        or archive alongside logs.
 *
 *  2. captureAsBytes() → Returns raw PNG bytes WITHOUT saving to disk.
 *                        Used by TestListener to embed the screenshot directly into
 *                        the ExtentReport HTML as a Base64-encoded inline image.
 *                        This keeps the report self-contained (no broken image links).
 *
 * Screenshots are timestamped so repeated failures on the same test don't overwrite
 * each other — each run produces a unique file.
 */
public class ScreenshotUtils {

    private static final Logger log = LogManager.getLogger(ScreenshotUtils.class);

    // All disk screenshots land in this folder (created automatically if missing).
    private static final String SCREENSHOT_DIR = "test-output/screenshots/";

    // Utility class — no instantiation needed.
    private ScreenshotUtils() {}

    /**
     * Captures the current browser viewport and saves it as a PNG file.
     *
     * How it works:
     *  1. Cast WebDriver to TakesScreenshot — Selenium's interface for screenshot capture.
     *  2. getScreenshotAs(FILE) writes the PNG to a temp file managed by Selenium.
     *  3. FileUtils.copyFile (Apache Commons IO) moves it to our named output path.
     *
     * @param testName  used as the filename prefix (e.g. "testSuccessfulLogin")
     * @return          path of the saved file, or null if capture failed
     */
    public static String capture(String testName) {
        WebDriver driver = DriverManager.getDriver();

        // Guard against calling this when no driver is active (e.g. in @AfterSuite).
        if (driver == null) {
            log.warn("Driver is null — cannot capture screenshot");
            return null;
        }

        try {
            // Create the output directory if it doesn't already exist.
            Files.createDirectories(Paths.get(SCREENSHOT_DIR));

            // Append a timestamp to make filenames unique per run.
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
            String filePath  = SCREENSHOT_DIR + testName + "_" + timestamp + ".png";

            // Selenium captures the screenshot into a temporary File object.
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

            // Copy from the temp location to our desired output path.
            FileUtils.copyFile(src, new File(filePath));

            log.info("Screenshot saved: {}", filePath);
            return filePath;

        } catch (IOException e) {
            log.error("Screenshot capture failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Captures the current browser viewport and returns it as raw byte array.
     *
     * This is used by TestListener on test failure:
     *   byte[] bytes = ScreenshotUtils.captureAsBytes();
     *   String base64 = Base64.getEncoder().encodeToString(bytes);
     *   extentTest.fail("Screenshot", MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
     *
     * The Base64 string is embedded directly in the HTML report, so the report
     * works as a single file without external image dependencies.
     *
     * @return PNG bytes, or an empty array if the driver is unavailable
     */
    public static byte[] captureAsBytes() {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) return new byte[0];
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }
}
