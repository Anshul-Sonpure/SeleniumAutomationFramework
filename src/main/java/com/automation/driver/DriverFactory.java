package com.automation.driver;

import com.automation.utils.ConfigReader;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * DriverFactory — Factory class responsible for creating and configuring
 * WebDriver instances for different browsers.
 *
 * FACTORY PATTERN:
 * ─────────────────
 * Instead of scattering "new ChromeDriver()" calls across test classes,
 * all browser setup is centralised here. To support a new browser (e.g. Safari),
 * you only change this one class — no test code needs to change.
 *
 * WEBDRIVERMANAGER:
 * ─────────────────
 * Previously developers had to manually download chromedriver.exe, geckodriver.exe,
 * etc. and keep them in sync with the installed browser version. WebDriverManager
 * (io.github.bonigarcia) automates this: it detects the installed browser version,
 * downloads the matching driver binary, and configures the system path — all at
 * runtime with a single .setup() call.
 */
public class DriverFactory {

    private static final Logger log = LogManager.getLogger(DriverFactory.class);

    // Private constructor — this class only exposes a static factory method.
    private DriverFactory() {}

    /**
     * Creates a fully configured WebDriver for the requested browser.
     *
     * The headless flag is read from config.properties (or overridden via
     * -Dheadless=true on the Maven command line). Headless mode runs the browser
     * without a visible UI window — essential for CI/CD pipelines that have no
     * display server.
     *
     * @param browser  browser name: "chrome", "firefox", or "edge" (case-insensitive)
     * @return         a ready-to-use WebDriver instance
     */
    public static WebDriver createDriver(String browser) {

        // Read headless flag once — shared by all browser branches below.
        boolean headless = ConfigReader.getBoolean("headless", false);

        switch (browser.trim().toLowerCase()) {

            // ── Firefox ───────────────────────────────────────────────────────
            case "firefox": {
                // WebDriverManager downloads geckodriver matching installed Firefox version.
                WebDriverManager.firefoxdriver().setup();

                FirefoxOptions opts = new FirefoxOptions();
                if (headless) opts.addArguments("--headless"); // Firefox headless flag
                log.info("Initialising FirefoxDriver (headless={})", headless);
                return new FirefoxDriver(opts);
            }

            // ── Edge ──────────────────────────────────────────────────────────
            case "edge": {
                // WebDriverManager downloads msedgedriver matching installed Edge version.
                WebDriverManager.edgedriver().setup();

                EdgeOptions opts = new EdgeOptions();
                if (headless) opts.addArguments("--headless");
                log.info("Initialising EdgeDriver (headless={})", headless);
                return new EdgeDriver(opts);
            }

            // ── Chrome (default) ──────────────────────────────────────────────
            case "chrome":
            default: {
                // WebDriverManager downloads chromedriver matching installed Chrome version.
                WebDriverManager.chromedriver().setup();

                ChromeOptions opts = new ChromeOptions();
                if (headless) opts.addArguments("--headless=new"); // "new" headless is the modern Chrome headless mode

                // --start-maximized          → open window fully maximised (consistent viewport)
                // --disable-notifications    → suppress "Allow notifications?" popup
                // --no-sandbox               → required when running as root (e.g. Docker containers)
                // --disable-dev-shm-usage    → prevents crashes in Docker due to small /dev/shm
                opts.addArguments(
                        "--start-maximized",
                        "--disable-notifications",
                        "--disable-popup-blocking",
                        "--no-sandbox",
                        "--disable-dev-shm-usage"
                );
                log.info("Initialising ChromeDriver (headless={})", headless);
                return new ChromeDriver(opts);
            }
        }
    }
}
