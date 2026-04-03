package com.automation.base;

import com.automation.driver.DriverFactory;
import com.automation.driver.DriverManager;
import com.automation.listeners.TestListener;
import com.automation.utils.ConfigReader;
import com.automation.utils.ExtentReportManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;

import java.time.Duration;

// Parent for all test classes — manages driver lifecycle and ExtentReport init/flush.
@Listeners(TestListener.class)
public class BaseTest {

    private static final Logger log = LogManager.getLogger(BaseTest.class);

    @BeforeSuite(alwaysRun = true)
    public void suiteSetup() {
        ExtentReportManager.getInstance();
        log.info("ExtentReport initialised");
    }

    // Fresh driver per test for full browser isolation; browser resolved from -Dbrowser= or config.properties.
    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        String targetBrowser = ConfigReader.get("browser", "chrome");
        log.info("Setting up WebDriver — browser: [{}]", targetBrowser);

        WebDriver driver = DriverFactory.createDriver(targetBrowser);
        DriverManager.setDriver(driver);

        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(ConfigReader.getInt("page.load.timeout", 30)));
        driver.manage().window().maximize();

        String baseUrl = ConfigReader.get("base.url", "");
        if (!baseUrl.isBlank()) {
            driver.get(baseUrl);
            log.info("Opened base URL: {}", baseUrl);
        }
    }

    // alwaysRun = true ensures cleanup even on test failure, preventing browser process leaks.
    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        WebDriver driver = DriverManager.getDriver();
        if (driver != null) {
            driver.quit();
            log.info("WebDriver closed");
        }
        DriverManager.removeDriver();
        ExtentReportManager.removeTest();
    }

    @AfterSuite(alwaysRun = true)
    public void suiteTearDown() {
        ExtentReportManager.flush();
        log.info("Suite completed — report flushed");
    }
}
