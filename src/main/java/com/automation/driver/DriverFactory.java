package com.automation.driver;

import com.automation.utils.ConfigReader;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.util.HashMap;
import java.util.Map;

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

    // Builds a Selenium Proxy pointed at ZAP when zap.enabled=true in config.
    // --ignore-certificate-errors lets Chrome accept ZAP's self-signed HTTPS intercept cert.
    private static Proxy buildZapProxy() {
        String zapAddr = ConfigReader.get("zap.host", "localhost")
                + ":" + ConfigReader.get("zap.port", "8080");
        Proxy proxy = new Proxy();
        proxy.setHttpProxy(zapAddr);
        proxy.setSslProxy(zapAddr);
        log.info("ZAP proxy configured at {}", zapAddr);
        return proxy;
    }

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

        boolean headless  = ConfigReader.getBoolean("headless",  false);
        boolean incognito = ConfigReader.getBoolean("incognito", false);
        boolean zapEnabled = ConfigReader.getBoolean("zap.enabled", false);

        switch (browser.trim().toLowerCase()) {

            // ── Firefox ───────────────────────────────────────────────────────
            case "firefox": {
                WebDriverManager.firefoxdriver().setup();

                FirefoxOptions opts = new FirefoxOptions();
                if (headless)    opts.addArguments("--headless");
                if (incognito)   opts.addArguments("-private");
                if (zapEnabled)  opts.setProxy(buildZapProxy());
                log.info("Initialising FirefoxDriver (headless={}, incognito={}, zap={})", headless, incognito, zapEnabled);
                return new FirefoxDriver(opts);
            }

            // ── Edge ──────────────────────────────────────────────────────────
            case "edge": {
                WebDriverManager.edgedriver().setup();

                EdgeOptions opts = new EdgeOptions();
                if (headless)    opts.addArguments("--headless");
                if (incognito)   opts.addArguments("--inprivate");
                if (zapEnabled) {
                    opts.setProxy(buildZapProxy());
                    opts.addArguments("--ignore-certificate-errors");
                }
                log.info("Initialising EdgeDriver (headless={}, incognito={}, zap={})", headless, incognito, zapEnabled);
                return new EdgeDriver(opts);
            }

            // ── Chrome (default) ──────────────────────────────────────────────
            case "chrome":
            default: {
                WebDriverManager.chromedriver().setup();

                ChromeOptions opts = new ChromeOptions();
                if (headless)  opts.addArguments("--headless=new");
                if (incognito) opts.addArguments("--incognito");

                // --start-maximized                → open window fully maximised (consistent viewport)
                // --disable-notifications          → suppress "Allow notifications?" popup
                // --no-sandbox                     → required when running as root (e.g. Docker containers)
                // --disable-dev-shm-usage          → prevents crashes in Docker due to small /dev/shm
                // --disable-features=...           → disables Chrome's password leak/breach detection;
                //                                    without this Chrome shows a "password found in data
                //                                    breach" notification bubble after login on sites that
                //                                    use well-known test passwords (e.g. saucedemo).
                opts.addArguments(
                        "--start-maximized",
                        "--disable-notifications",
                        "--disable-popup-blocking",
                        "--no-sandbox",
                        "--disable-dev-shm-usage",
                        "--disable-features=PasswordLeakDetection"
                );

                // Disable Chrome's password manager entirely so it never prompts to save
                // credentials or warn about breached passwords during test runs.
                Map<String, Object> prefs = new HashMap<>();
                prefs.put("credentials_enable_service", false);
                prefs.put("profile.password_manager_enabled", false);
                opts.setExperimentalOption("prefs", prefs);

                if (zapEnabled) {
                    opts.setProxy(buildZapProxy());
                    // Allow Chrome to trust ZAP's self-signed certificate for HTTPS interception.
                    opts.addArguments("--ignore-certificate-errors");
                }

                log.info("Initialising ChromeDriver (headless={}, incognito={}, zap={})", headless, incognito, zapEnabled);
                return new ChromeDriver(opts);
            }
        }
    }
}
