package com.automation.tests;

import com.automation.base.BaseTest;
import com.automation.pages.CartPage;
import com.automation.pages.CheckoutInfoPage;
import com.automation.pages.LoginPage;
import com.automation.pages.ProductsPage;
import com.automation.security.ZapManager;
import com.automation.utils.ConfigReader;
import com.automation.utils.ExtentReportManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.zaproxy.clientapi.core.Alert;

import java.util.List;

/**
 * ZapPassiveScanTest — drives a full user journey through OWASP ZAP acting as a
 * proxy, then queries the ZAP API to assert on passive scan findings.
 *
 * PRE-REQUISITES:
 *   1. OWASP ZAP installed and running as a daemon:
 *        zap.sh -daemon -port 8080 -host 0.0.0.0
 *   2. zap.enabled=true in config.properties (routes browser traffic through ZAP)
 *   3. zap.host and zap.port match the running ZAP instance
 *
 * If ZAP is not reachable the entire class is skipped with an explanatory message.
 *
 * PASSIVE vs ACTIVE scanning:
 *   Passive — ZAP inspects traffic it observes without sending extra requests.
 *             Safe for production-like environments; what this test uses.
 *   Active  — ZAP sends its own attack requests (fuzzing, injections).
 *             Destructive; should only run in isolated test environments.
 *
 * Run standalone:  mvn test -Dgroups=zap -Dzap.enabled=true
 */
public class ZapPassiveScanTest extends BaseTest {

    private static final Logger log = LogManager.getLogger(ZapPassiveScanTest.class);

    private ZapManager zap;

    @BeforeClass(alwaysRun = true)
    public void initZap() {
        if (!ConfigReader.getBoolean("zap.enabled", false)) {
            throw new SkipException(
                    "ZAP scan skipped — set zap.enabled=true in config.properties and start ZAP: "
                    + "zap.sh -daemon -port 8080");
        }

        String zapHost = ConfigReader.get("zap.host", "localhost");
        int    zapPort = ConfigReader.getInt("zap.port", 8080);
        zap = new ZapManager(zapHost, zapPort);

        if (!zap.isRunning()) {
            throw new SkipException(
                    "ZAP is not reachable at " + zapHost + ":" + zapPort
                    + " — start it with: zap.sh -daemon -port " + zapPort);
        }

        try {
            zap.newSession();
        } catch (Exception e) {
            throw new SkipException("ZAP session reset failed: " + e.getMessage());
        }

        log.info("ZAP connected at {}:{}", zapHost, zapPort);
    }

    /**
     * Navigates through the full purchase flow so ZAP can passively inspect
     * all request/response pairs across login, browse, cart, and checkout pages.
     */
    @Test(description = "Browse application through ZAP proxy to collect passive scan traffic",
          groups = {"security", "zap"})
    public void browseAppThroughZap() {
        ExtentReportManager.getTest().info("Routing Selenium session through ZAP proxy for passive scan");

        LoginPage loginPage = new LoginPage();
        ProductsPage productsPage = loginPage.login(
                ConfigReader.get("app.username", "standard_user"),
                ConfigReader.get("app.password", "secret_sauce"));

        productsPage.addToCartByName("Sauce Labs Backpack");
        productsPage.addToCartByName("Sauce Labs Bike Light");

        CartPage cartPage = productsPage.clickCart();
        CheckoutInfoPage checkoutInfoPage = cartPage.clickCheckout();
        checkoutInfoPage.fillAndContinue("Test", "User", "10001");

        ExtentReportManager.getTest().pass("Application fully browsed — ZAP captured all traffic");
        log.info("Browse phase complete — ZAP has captured application traffic");
    }

    /**
     * Waits for ZAP's passive scan queue to drain, then asserts that no HIGH risk
     * alerts were found. Exports the full ZAP HTML report to test-output/.
     */
    @Test(description = "ZAP passive scan must find zero HIGH risk alerts",
          groups = {"security", "zap"},
          dependsOnMethods = {"browseAppThroughZap"})
    public void assertNoHighRiskAlerts() throws Exception {
        zap.waitForPassiveScan();

        String targetUrl = ConfigReader.get("base.url", "https://www.saucedemo.com");
        List<Alert> alerts = zap.getAlerts(targetUrl);

        long highCount   = zap.countByRisk(alerts, Alert.Risk.High);
        long mediumCount = zap.countByRisk(alerts, Alert.Risk.Medium);
        long lowCount    = zap.countByRisk(alerts, Alert.Risk.Low);
        long infoCount   = zap.countByRisk(alerts, Alert.Risk.Informational);

        log.info("ZAP alerts — High: {}, Medium: {}, Low: {}, Info: {}",
                highCount, mediumCount, lowCount, infoCount);
        ExtentReportManager.getTest().info(
                "ZAP alerts — High: " + highCount
                + " | Medium: " + mediumCount
                + " | Low: " + lowCount
                + " | Info: " + infoCount);

        try {
            zap.exportHtmlReport("test-output/zap-passive-scan-report.html");
            ExtentReportManager.getTest().info(
                    "ZAP HTML report: test-output/zap-passive-scan-report.html");
        } catch (Exception e) {
            log.warn("Could not export ZAP report: {}", e.getMessage());
        }

        Assert.assertEquals(highCount, 0,
                highCount + " HIGH risk alert(s) found by ZAP passive scan. "
                + "See test-output/zap-passive-scan-report.html for details.");
    }
}
