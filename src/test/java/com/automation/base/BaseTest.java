package com.automation.base;

import com.automation.driver.DriverFactory;
import com.automation.driver.DriverManager;
import com.automation.listeners.TestListener;
import com.automation.utils.ConfigReader;
import com.automation.utils.ExtentReportManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.*;

import java.time.Duration;

/**
 * BaseTest — Parent class for every test class in the framework.
 *
 * RESPONSIBILITIES:
 * ──────────────────
 *  • Initialise WebDriver before each test method (@BeforeMethod).
 *  • Quit WebDriver and clean up ThreadLocals after each test (@AfterMethod).
 *  • Initialise and flush ExtentReports at suite boundaries.
 *
 * TESTNG LIFECYCLE ORDER (per test method):
 * ───────────────────────────────────────────
 *   @BeforeSuite  (once, before the suite)
 *   @BeforeMethod (before each @Test)
 *        @Test
 *   @AfterMethod  (after each @Test — runs even if the test failed)
 *   @AfterSuite   (once, after the suite)
 *
 * WHY @BeforeMethod (not @BeforeClass)?
 * ───────────────────────────────────────
 * A fresh WebDriver per @Test method ensures:
 *  • Complete browser isolation — cookies/session from test A cannot affect test B.
 *  • Clean slate for each test — no state leaks.
 *  • Safe parallel execution — each thread gets its own driver instance.
 *
 * @Listeners(TestListener.class):
 * ─────────────────────────────────
 * Attaches TestListener to every subclass automatically. The listener handles
 * ExtentReport logging and screenshot capture on failure — keeping test classes
 * free of any reporting code.
 */
@Listeners(TestListener.class)
public class BaseTest {

    private static final Logger log = LogManager.getLogger(BaseTest.class);

    /**
     * Runs once before any test in the suite starts.
     * Initialises the ExtentReport singleton so the HTML file is created
     * before TestListener.onTestStart() tries to write to it.
     */
    @BeforeSuite(alwaysRun = true)
    public void suiteSetup() {
        ExtentReportManager.getInstance();
        log.info("ExtentReport initialised — report will be at test-output/ExtentReport.html");
    }

    /**
     * Runs before EACH @Test method. Creates a fresh WebDriver and opens the base URL.
     *
     * @Parameters("browser") — TestNG injects the browser value from testng.xml's
     *   <parameter name="browser" value="chrome"/> element.
     *
     * @Optional — Makes the parameter non-mandatory. If testng.xml doesn't provide
     *   the parameter (e.g. when running a single test from IDE), 'browser' will be
     *   null and we fall back to config.properties, then to "chrome".
     *
     * BROWSER RESOLUTION PRIORITY (highest → lowest):
     *   1. -Dbrowser=firefox  (Maven/CLI system property)
     *   2. <parameter> in testng.xml
     *   3. browser= in config.properties
     *   4. Hardcoded default "chrome"
     */
    @BeforeMethod(alwaysRun = true)
    @Parameters("browser")
    public void setUp(@Optional String browser) {

        // Determine the browser to use, falling through the priority chain.
        String targetBrowser = (browser != null && !browser.isBlank())
                ? browser
                : ConfigReader.get("browser", "chrome");

        log.info("Setting up WebDriver — browser: [{}]", targetBrowser);

        // Create the driver via factory and register it in the ThreadLocal holder.
        WebDriver driver = DriverFactory.createDriver(targetBrowser);
        DriverManager.setDriver(driver);

        // Implicit wait: Selenium will poll for up to N seconds when findElement()
        // doesn't find the element immediately. A small value (5 s) prevents masking
        // real timing issues while still handling minor render delays.
        driver.manage().timeouts()
                .implicitlyWait(Duration.ofSeconds(ConfigReader.getInt("implicit.wait", 5)));

        // Page load timeout: if a page takes longer than this to load, WebDriver throws.
        driver.manage().timeouts()
                .pageLoadTimeout(Duration.ofSeconds(ConfigReader.getInt("page.load.timeout", 30)));

        // Maximise window for a consistent viewport size across all tests.
        driver.manage().window().maximize();

        // Navigate to the base URL if one is configured.
        // Individual tests can override this by calling driver.get() themselves.
        String baseUrl = ConfigReader.get("base.url", "");
        if (!baseUrl.isBlank()) {
            driver.get(baseUrl);
            log.info("Opened base URL: {}", baseUrl);
        }
    }

    /**
     * Runs after EACH @Test method, regardless of pass/fail (alwaysRun = true).
     *
     * alwaysRun = true is critical here — if this were false, a failing test would
     * leave its driver open, leaking browser processes and potentially blocking
     * subsequent tests in a parallel run.
     *
     * Cleanup order:
     *  1. Quit the browser (releases the OS process).
     *  2. Remove the driver from ThreadLocal (prevents memory leak).
     *  3. Remove ExtentTest from ThreadLocal (prevents stale references).
     */
    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        WebDriver driver = DriverManager.getDriver();

        if (driver != null) {
            driver.quit(); // quit() closes ALL windows and ends the WebDriver session.
            log.info("WebDriver closed");
        }

        // Clean up ThreadLocal slots for this thread.
        // Must be done even if driver was null to prevent stale state on thread reuse.
        DriverManager.removeDriver();
        ExtentReportManager.removeTest();
    }

    /**
     * Runs once after all tests in the suite have completed.
     * flush() writes all buffered ExtentReport data to the HTML file on disk.
     * Without this, the report is empty or incomplete.
     */
    @AfterSuite(alwaysRun = true)
    public void suiteTearDown() {
        ExtentReportManager.flush();
        log.info("Test suite completed. Report available at: test-output/ExtentReport.html");
    }
}
