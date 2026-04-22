package com.automation.tests;

import com.automation.base.BaseTest;
import com.automation.driver.DriverManager;
import com.automation.pages.CartPage;
import com.automation.pages.CheckoutInfoPage;
import com.automation.pages.LoginPage;
import com.automation.pages.ProductsPage;
import com.automation.security.SecurityPayloads;
import com.automation.utils.ConfigReader;
import com.automation.utils.ExtentReportManager;
import org.openqa.selenium.NoAlertPresentException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * InputValidationSecurityTest — verifies that the application correctly rejects
 * or neutralises malicious input without executing it.
 *
 * WHAT IS TESTED:
 *   1. XSS in login fields  — payload entered into username/password fields should
 *      not trigger a JavaScript alert dialog (reflected XSS indicator).
 *   2. SQLi in login fields — injection strings should produce an error, never
 *      bypass authentication and reach the inventory page.
 *   3. XSS in checkout form — payload in first/last name fields must not execute
 *      as JavaScript when rendered back by the application.
 *
 * NOTE: These tests detect obvious (reflected) XSS and basic auth bypass.
 * They do NOT cover stored XSS, blind SQLi, or second-order injection —
 * those require full DAST tooling such as OWASP ZAP (see ZapPassiveScanTest).
 *
 * DataProviders use parallel=false to keep each payload independent across
 * the thread pool; BaseTest.setUp() already isolates each @Test in its own browser.
 */
public class InputValidationSecurityTest extends BaseTest {

    // ── Login field XSS ──────────────────────────────────────────────────────

    @Test(dataProvider = "xssPayloads",
          description = "XSS payload in username field should not trigger JavaScript execution",
          groups = {"security", "injection"})
    public void testXssInUsernameField(String payload) {
        ExtentReportManager.getTest().info("XSS payload under test: " + payload);

        LoginPage loginPage = new LoginPage();
        loginPage.enterUsername(payload);
        loginPage.enterPassword("anypassword");
        loginPage.clickLoginButton();

        Assert.assertFalse(isAlertPresent(),
                "JavaScript alert fired for XSS payload in username field: " + payload);
        Assert.assertTrue(loginPage.isErrorMessageDisplayed(),
                "Expected a validation error message for invalid input, payload: " + payload);

        ExtentReportManager.getTest().pass("No JS execution for payload: " + payload);
    }

    // ── Login field SQL injection ─────────────────────────────────────────────

    @Test(dataProvider = "sqliPayloads",
          description = "SQL injection in username should not bypass authentication",
          groups = {"security", "injection"})
    public void testSqlInjectionInUsernameField(String payload) {
        ExtentReportManager.getTest().info("SQLi payload under test: " + payload);

        LoginPage loginPage = new LoginPage();
        loginPage.enterUsername(payload);
        loginPage.enterPassword("anypassword");
        loginPage.clickLoginButton();

        // An auth bypass would navigate away from the login page to /inventory.html.
        String currentUrl = DriverManager.getDriver().getCurrentUrl();
        Assert.assertFalse(currentUrl.contains("/inventory"),
                "SQLi payload bypassed authentication — URL reached: " + currentUrl
                + " | payload: " + payload);

        // The application must report an error, not silently succeed.
        Assert.assertTrue(loginPage.isErrorMessageDisplayed(),
                "Expected a login error for SQLi payload, but no error was shown: " + payload);

        ExtentReportManager.getTest().pass("Auth bypass blocked for payload: " + payload);
    }

    // ── Checkout form XSS ────────────────────────────────────────────────────

    @Test(dataProvider = "xssPayloads",
          description = "XSS payload in checkout name fields should not trigger JavaScript execution",
          groups = {"security", "injection"})
    public void testXssInCheckoutFormFields(String payload) {
        ExtentReportManager.getTest().info("XSS payload in checkout form: " + payload);

        LoginPage loginPage = new LoginPage();
        ProductsPage productsPage = loginPage.login(
                ConfigReader.get("app.username", "standard_user"),
                ConfigReader.get("app.password", "secret_sauce"));

        productsPage.addToCartByName("Sauce Labs Backpack");
        CartPage cartPage = productsPage.clickCart();
        CheckoutInfoPage checkoutInfoPage = cartPage.clickCheckout();

        checkoutInfoPage.enterFirstName(payload);
        checkoutInfoPage.enterLastName(payload);
        checkoutInfoPage.enterZipCode("10001");

        // Check for JS alert fired during field entry or on next render.
        Assert.assertFalse(isAlertPresent(),
                "JavaScript alert fired for XSS payload in checkout form: " + payload);

        ExtentReportManager.getTest().pass("No JS execution in checkout form for payload: " + payload);
    }

    // ── DataProviders ─────────────────────────────────────────────────────────

    @DataProvider(name = "xssPayloads", parallel = false)
    public Object[][] xssPayloads() {
        return toDataProvider(SecurityPayloads.XSS_PAYLOADS);
    }

    @DataProvider(name = "sqliPayloads", parallel = false)
    public Object[][] sqliPayloads() {
        return toDataProvider(SecurityPayloads.SQLI_PAYLOADS);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    // Returns true and dismisses the alert if one is open; false otherwise.
    private boolean isAlertPresent() {
        try {
            DriverManager.getDriver().switchTo().alert().dismiss();
            return true;
        } catch (NoAlertPresentException e) {
            return false;
        }
    }

    private static Object[][] toDataProvider(String[] payloads) {
        Object[][] data = new Object[payloads.length][1];
        for (int i = 0; i < payloads.length; i++) {
            data[i][0] = payloads[i];
        }
        return data;
    }
}
