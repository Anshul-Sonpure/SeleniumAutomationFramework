package com.automation.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Singleton managing one ExtentReports instance per run.
// ThreadLocal<ExtentTest> isolates per-thread test nodes for parallel safety.
public class ExtentReportManager {

    private static volatile ExtentReports extentReports;
    private static final ThreadLocal<ExtentTest> testThread = new ThreadLocal<>();

    // Each run gets its own timestamped file — preserves history across executions.
    private static final String REPORT_DIR = "test-output/reports/";
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private ExtentReportManager() {}

    // Double-checked locking ensures one instance regardless of parallel thread starts.
    public static ExtentReports getInstance() {
        if (extentReports == null) {
            synchronized (ExtentReportManager.class) {
                if (extentReports == null) {
                    String timestamp = LocalDateTime.now().format(TS_FORMAT);
                    String reportPath = REPORT_DIR + "ExtentReport_" + timestamp + ".html";

                    ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
                    spark.config().setReportName("UI Automation Report");
                    spark.config().setDocumentTitle("Test Results");
                    spark.config().setTheme(Theme.DARK);
                    spark.config().setEncoding("utf-8");

                    extentReports = new ExtentReports();
                    extentReports.attachReporter(spark);

                    String rawBrowser = ConfigReader.get("browser", "chrome");
                    String browser = (rawBrowser == null || rawBrowser.isBlank()) ? "chrome" : rawBrowser.trim();
                    String browserDisplay = Character.toUpperCase(browser.charAt(0)) + browser.substring(1).toLowerCase();

                    extentReports.setSystemInfo("Framework",       "Selenium 4 + TestNG");
                    extentReports.setSystemInfo("OS",              System.getProperty("os.name"));
                    extentReports.setSystemInfo("Java",            System.getProperty("java.version"));
                    extentReports.setSystemInfo("Browser",         browserDisplay);
                    extentReports.setSystemInfo("Executed By",     System.getProperty("user.name"));
                    extentReports.setSystemInfo("Executed",        timestamp.replace("_", " ").replace("-", ":"));
                }
            }
        }
        return extentReports;
    }

    public static ExtentTest getTest()            { return testThread.get(); }
    public static void setTest(ExtentTest test)   { testThread.set(test); }
    public static void removeTest()               { testThread.remove(); }

    // Must be called at suite end — flushes buffered results to the HTML file.
    public static void flush() {
        if (extentReports != null) extentReports.flush();
    }
}
