package com.automation.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

/**
 * ExtentReportManager — Manages the lifecycle of ExtentReports for the test suite.
 *
 * EXTENTREPORTS OVERVIEW:
 * ────────────────────────
 * ExtentReports is a reporting library that generates rich HTML test reports with
 * pass/fail status, logs, screenshots, and system info — far more readable than
 * the default TestNG HTML report.
 *
 * Key classes:
 *  • ExtentReports       — The master report object. Holds all tests. Flush at suite end.
 *  • ExtentTest          — Represents a single test case in the report. Created per test method.
 *  • ExtentSparkReporter — The HTML renderer; writes to a .html file on disk.
 *
 * SINGLETON PATTERN (Double-Checked Locking):
 * ─────────────────────────────────────────────
 * We want exactly ONE ExtentReports instance per suite run — multiple instances
 * would produce multiple partial report files. The double-checked locking pattern
 * ensures thread-safety during parallel test initialisation without the overhead
 * of synchronising every single call to getInstance().
 *
 *   First check  (outside synchronized): fast path — avoids locking if already created.
 *   Second check (inside  synchronized): safety net — prevents two threads that both
 *                                        passed the first check from creating duplicates.
 *
 * ThreadLocal<ExtentTest>:
 * ─────────────────────────
 * Each running test method needs its own ExtentTest node in the report.
 * With parallel execution, multiple threads call onTestStart simultaneously.
 * ThreadLocal ensures each thread's ExtentTest is isolated — Thread A's log
 * entries don't accidentally appear under Thread B's test node.
 */
public class ExtentReportManager {

    // volatile ensures that the fully-constructed object is visible to all threads
    // immediately after assignment (guards against CPU cache / instruction reordering).
    private static volatile ExtentReports extentReports;

    // One ExtentTest per thread — safe for parallel execution.
    private static final ThreadLocal<ExtentTest> testThread = new ThreadLocal<>();

    // Output path for the generated HTML report.
    private static final String REPORT_PATH = "test-output/ExtentReport.html";

    // Utility class — prevent instantiation.
    private ExtentReportManager() {}

    /**
     * Returns the single ExtentReports instance, creating it on first call.
     * Thread-safe via double-checked locking.
     *
     * The reporter is configured here (theme, title, system info). These settings
     * are written once into the report header and appear on every run.
     */
    public static ExtentReports getInstance() {
        if (extentReports == null) {                         // First check — no locking overhead on subsequent calls
            synchronized (ExtentReportManager.class) {
                if (extentReports == null) {                 // Second check — only one thread creates the object
                    ExtentSparkReporter spark = new ExtentSparkReporter(REPORT_PATH);
                    spark.config().setReportName("UI Automation Report");
                    spark.config().setDocumentTitle("Test Results");
                    spark.config().setTheme(Theme.DARK);      // Dark theme for modern look
                    spark.config().setEncoding("utf-8");       // Support international characters

                    extentReports = new ExtentReports();
                    extentReports.attachReporter(spark);       // Link the HTML renderer

                    // System info appears in the report's environment section.
                    extentReports.setSystemInfo("Framework", "Selenium 4 + TestNG");
                    extentReports.setSystemInfo("OS",         System.getProperty("os.name"));
                    extentReports.setSystemInfo("Java",       System.getProperty("java.version"));
                    extentReports.setSystemInfo("Browser",    ConfigReader.get("browser", "chrome"));
                }
            }
        }
        return extentReports;
    }

    /**
     * Returns the ExtentTest for the currently running thread.
     * Called in TestListener.onTestSuccess/Failure/Skipped to add log entries.
     */
    public static ExtentTest getTest() {
        return testThread.get();
    }

    /**
     * Stores the ExtentTest for the currently running thread.
     * Called in TestListener.onTestStart() right after creating the test node.
     */
    public static void setTest(ExtentTest test) {
        testThread.set(test);
    }

    /**
     * Removes the ExtentTest from the current thread's ThreadLocal slot.
     * Called in BaseTest.tearDown() to prevent memory leaks in thread pools.
     */
    public static void removeTest() {
        testThread.remove();
    }

    /**
     * Writes all buffered test results to disk and closes the report file.
     *
     * IMPORTANT: flush() MUST be called at the end of the suite.
     * Without it, the HTML file is either incomplete or empty — ExtentReports
     * buffers writes in memory until flush() serialises them to the .html file.
     * Called in BaseTest.suiteTearDown() via @AfterSuite.
     */
    public static void flush() {
        if (extentReports != null) extentReports.flush();
    }
}
