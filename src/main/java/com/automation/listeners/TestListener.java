package com.automation.listeners;

import com.automation.utils.ExtentReportManager;
import com.automation.utils.ScreenshotUtils;
import com.automation.utils.VideoRecorder;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.util.Base64;

// Bridges TestNG lifecycle events to ExtentReports and Log4j2.
// Registered via @Listeners on BaseTest so all subclasses are covered automatically.
public class TestListener implements ITestListener {

    private static final Logger log = LogManager.getLogger(TestListener.class);

    @Override
    public void onStart(ITestContext ctx) {
        log.info("===== Suite '{}' started =====", ctx.getName());
        ExtentReportManager.getInstance();
    }

    @Override
    public void onFinish(ITestContext ctx) {
        log.info("===== Suite '{}' finished =====", ctx.getName());
        ExtentReportManager.flush();
    }

    @Override
    public void onTestStart(ITestResult result) {
        String methodName  = result.getMethod().getMethodName();
        String description = result.getMethod().getDescription();
        log.info("[START] {}", methodName);
        // Create one ExtentTest node per thread; ThreadLocal keeps parallel threads isolated.
        ExtentTest test = ExtentReportManager.getInstance().createTest(methodName, description);
        ExtentReportManager.setTest(test);
        VideoRecorder.start();
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String methodName = result.getMethod().getMethodName();
        log.info("[PASS]  {}", methodName);
        String gifPath = VideoRecorder.stop(methodName);
        ExtentTest test = ExtentReportManager.getTest();
        test.log(Status.PASS, "Test passed successfully");
        embedGif(test, gifPath);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String methodName = result.getMethod().getMethodName();
        log.error("[FAIL]  {} — {}", methodName, result.getThrowable().getMessage());
        String gifPath = VideoRecorder.stop(methodName);

        ExtentTest test = ExtentReportManager.getTest();
        test.log(Status.FAIL, result.getThrowable());

        // Embed screenshot as Base64 inline image — avoids broken links when report is moved.
        byte[] screenshotBytes = ScreenshotUtils.captureAsBytes();
        if (screenshotBytes.length > 0) {
            String base64 = Base64.getEncoder().encodeToString(screenshotBytes);
            test.fail("Screenshot on failure",
                    MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
        }
        embedGif(test, gifPath);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String methodName = result.getMethod().getMethodName();
        log.warn("[SKIP]  {}", methodName);
        VideoRecorder.stop(methodName); // cleanup only — no embed for skipped tests
        ExtentTest test = ExtentReportManager.getTest();
        if (test != null) {
            test.log(Status.SKIP, "Test skipped: " + result.getThrowable());
        }
    }

    private void embedGif(ExtentTest test, String gifPath) {
        if (gifPath == null) return;
        // Relative path from test-output/reports/ to test-output/videos/
        String rel = "../videos/" + new File(gifPath).getName();
        test.info("<div><img src='" + rel + "' style='max-width:100%;border:1px solid #555' "
                + "alt='Test recording'/></div>");
    }
}
