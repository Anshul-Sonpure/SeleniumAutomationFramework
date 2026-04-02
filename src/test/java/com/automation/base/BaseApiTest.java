package com.automation.base;

import com.automation.listeners.TestListener;
import com.automation.utils.ExtentReportManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;

// Parent class for all API test classes.
// Does NOT extend BaseTest — no WebDriver is created for API tests.
// Shares TestListener with BaseTest; all existing null-driver guards in
// ScreenshotUtils and VideoRecorder make this safe with zero code changes.
@Listeners(TestListener.class)
public class BaseApiTest {

    private static final Logger log = LogManager.getLogger(BaseApiTest.class);

    @BeforeSuite(alwaysRun = true)
    public void suiteSetup() {
        // Idempotent — safe even when BaseTest already called getInstance() in the same suite.
        ExtentReportManager.getInstance();
        log.info("ExtentReport initialised for API test suite");
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp(ITestResult result) {
        log.info("Starting API test: {}", result.getMethod().getMethodName());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        ExtentReportManager.removeTest();
    }

    @AfterSuite(alwaysRun = true)
    public void suiteTearDown() {
        ExtentReportManager.flush();
        log.info("API suite completed — report flushed");
    }
}
