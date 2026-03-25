package com.automation.listeners;

import com.automation.utils.ExtentReportManager;
import com.automation.utils.ScreenshotUtils;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Base64;

/**
 * TestListener — Bridges TestNG test lifecycle events to ExtentReports and Log4j2.
 *
 * HOW TESTNG LISTENERS WORK:
 * ───────────────────────────
 * TestNG fires lifecycle events (start, pass, fail, skip, suite start/finish) and
 * calls the matching methods on any class that implements ITestListener.
 * By centralising reporting here, test classes stay clean — no report boilerplate
 * in individual @Test methods.
 *
 * REGISTRATION:
 * ──────────────
 * This listener is registered via @Listeners(TestListener.class) on BaseTest.
 * Because BaseTest is the parent of every test class, the listener is automatically
 * active for all tests. It is also declared in testng.xml as a fallback for suites
 * that don't go through BaseTest.
 *
 * THREAD SAFETY:
 * ───────────────
 * Each callback receives an ITestResult that identifies which test method fired the
 * event. We use ExtentReportManager.getTest() (backed by ThreadLocal) to retrieve
 * the correct ExtentTest node for that thread — safe for parallel execution.
 */
public class TestListener implements ITestListener {

    private static final Logger log = LogManager.getLogger(TestListener.class);

    /**
     * Fired once when the entire test suite begins (before any test class is loaded).
     * We initialise ExtentReports here to ensure the report file is ready before
     * the first onTestStart() call.
     */
    @Override
    public void onStart(ITestContext ctx) {
        log.info("===== Suite '{}' started =====", ctx.getName());
        ExtentReportManager.getInstance(); // Initialise report (idempotent — safe to call multiple times)
    }

    /**
     * Fired once when the entire suite finishes.
     * flush() serialises all buffered results to the HTML file on disk.
     * Without this call, the report will be empty or incomplete.
     */
    @Override
    public void onFinish(ITestContext ctx) {
        log.info("===== Suite '{}' finished =====", ctx.getName());
        ExtentReportManager.flush();
    }

    /**
     * Fired immediately before each @Test method executes.
     *
     * We create a new ExtentTest node for this test and store it in the ThreadLocal
     * so that onTestSuccess/Failure/Skipped can retrieve it without needing a direct
     * reference — critical for parallel tests where multiple threads are running
     * simultaneously.
     */
    @Override
    public void onTestStart(ITestResult result) {
        String methodName  = result.getMethod().getMethodName();
        String description = result.getMethod().getDescription(); // from @Test(description="...")

        log.info("[START] {}", methodName);

        // Create a new test node in the report for this specific test method.
        ExtentTest test = ExtentReportManager.getInstance()
                .createTest(methodName, description);

        // Store in ThreadLocal so this thread's callbacks can retrieve it later.
        ExtentReportManager.setTest(test);
    }

    /**
     * Fired when a @Test method completes without any assertion failure or exception.
     * Marks the test as PASS in the report.
     */
    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("[PASS]  {}", result.getMethod().getMethodName());
        ExtentReportManager.getTest().log(Status.PASS, "Test passed successfully");
    }

    /**
     * Fired when a @Test method throws an exception (AssertionError, RuntimeException, etc.).
     *
     * We do two things here:
     *  1. Log the full exception stack trace in the report (so the exact failure reason is captured).
     *  2. Capture a screenshot and embed it as a Base64 inline image in the report.
     *     Embedding avoids broken image links if the report is moved to a different machine.
     */
    @Override
    public void onTestFailure(ITestResult result) {
        String methodName = result.getMethod().getMethodName();
        log.error("[FAIL]  {} — {}", methodName, result.getThrowable().getMessage());

        ExtentTest test = ExtentReportManager.getTest();

        // Log the exception with full stack trace.
        test.log(Status.FAIL, result.getThrowable());

        // Capture screenshot and embed as Base64 inline image.
        byte[] screenshotBytes = ScreenshotUtils.captureAsBytes();
        if (screenshotBytes.length > 0) {
            // Base64 encodes binary PNG bytes into an ASCII string that can be
            // embedded directly inside an HTML <img src="data:image/png;base64,..."> tag.
            String base64 = Base64.getEncoder().encodeToString(screenshotBytes);
            test.fail("Screenshot on failure",
                    MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
        }
    }

    /**
     * Fired when a @Test method is skipped (e.g. a dependency test failed, or
     * the test has a @Test(enabled=false) annotation).
     * Marks it as SKIP in the report so it's visible without cluttering the pass count.
     */
    @Override
    public void onTestSkipped(ITestResult result) {
        log.warn("[SKIP]  {}", result.getMethod().getMethodName());
        ExtentTest test = ExtentReportManager.getTest();
        if (test != null) {
            test.log(Status.SKIP, "Test skipped: " + result.getThrowable());
        }
    }
}
