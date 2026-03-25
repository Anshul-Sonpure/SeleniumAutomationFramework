package com.automation.tests;

import com.automation.base.BaseTest;
import com.automation.driver.DriverManager;
import com.automation.utils.ExtentReportManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * SampleLoginTest — Demonstrates framework usage with real-world test scenarios.
 *
 * TARGET APPLICATION: https://www.saucedemo.com
 *   A publicly available demo e-commerce site purpose-built for Selenium practice.
 *   Known credentials:
 *     standard_user   / secret_sauce  → login succeeds
 *     locked_out_user / secret_sauce  → login blocked with error message
 *     invalid_user    / wrong_pass    → shows generic error
 *
 * STRUCTURE:
 * ───────────
 * • Extends BaseTest — inherits driver setup/teardown and ExtentReport integration.
 * • Locators are declared as class-level fields (not local variables) so they are
 *   defined once and reused across test methods. If the UI changes, update in one place.
 * • Each @Test method is independent — it navigates to the page itself and does not
 *   rely on another test having run first (tests should never depend on order).
 */
public class SampleLoginTest extends BaseTest {

    // ── Locators ──────────────────────────────────────────────────────────────
    // Using By constants as fields keeps locator logic separate from test logic.
    // Change a locator here and every test method using it is automatically updated.

    /** Input field for the username — identified by its HTML id attribute. */
    private final By usernameField = By.id("user-name");

    /** Input field for the password. */
    private final By passwordField = By.id("password");

    /** The "Login" submit button. */
    private final By loginButton   = By.id("login-button");

    /**
     * Page title element displayed after successful login (shows "Products").
     * Located by CSS class name — a stable locator on this page.
     */
    private final By pageTitle     = By.className("title");

    /**
     * Error banner shown when login fails.
     * Located using a data-test attribute — these are the most stable locators
     * because they are specifically added for automation and less likely to change
     * due to styling or layout refactors.
     */
    private final By errorMessage  = By.cssSelector("[data-test='error']");

    // ── Test methods ──────────────────────────────────────────────────────────

    /**
     * HAPPY PATH: Valid credentials should redirect to the Products page.
     *
     * Steps:
     *  1. Navigate to the login page.
     *  2. Enter valid username and password.
     *  3. Click Login.
     *  4. Assert the page title is "Products" (confirming successful login).
     *
     * We log the page title to ExtentReport so the report shows the actual value
     * seen at runtime — useful when diagnosing failures in CI.
     */
    @Test(description = "Successful login with valid credentials lands on Products page")
    public void testSuccessfulLogin() {
        WebDriver driver = DriverManager.getDriver();
        driver.get("https://www.saucedemo.com");

        driver.findElement(usernameField).sendKeys("standard_user");
        driver.findElement(passwordField).sendKeys("secret_sauce");
        driver.findElement(loginButton).click();

        // Capture the actual page title for the report log.
        String title = driver.findElement(pageTitle).getText();
        ExtentReportManager.getTest().info("Page title after login: " + title);

        // Assert the expected value. TestNG will mark the test FAIL and throw
        // AssertionError if the actual value doesn't match — TestListener will
        // capture a screenshot automatically when that happens.
        Assert.assertEquals(title, "Products",
                "Expected to land on 'Products' page after valid login");
    }

    /**
     * NEGATIVE PATH: Invalid credentials should display an error message.
     *
     * We verify that the error banner is visible — we don't assert the exact
     * text because error messages are more prone to copy changes. Asserting
     * visibility makes the test robust to minor wording updates.
     */
    @Test(description = "Invalid credentials display an error message")
    public void testInvalidLogin() {
        WebDriver driver = DriverManager.getDriver();
        driver.get("https://www.saucedemo.com");

        driver.findElement(usernameField).sendKeys("wrong_user");
        driver.findElement(passwordField).sendKeys("wrong_pass");
        driver.findElement(loginButton).click();

        // isDisplayed() returns true/false without throwing — if the element is
        // absent from the DOM the assertion fails with a clear message.
        Assert.assertTrue(
                driver.findElement(errorMessage).isDisplayed(),
                "Error message was not displayed for invalid credentials"
        );
    }

    /**
     * EDGE CASE: A known locked-out user should see a specific error mentioning "locked out".
     *
     * We assert on partial text content (contains) rather than an exact match —
     * this makes the test tolerant of whitespace or punctuation changes while
     * still verifying the key information the user would see.
     */
    @Test(description = "Locked-out user sees a 'locked out' error message")
    public void testLockedOutUser() {
        WebDriver driver = DriverManager.getDriver();
        driver.get("https://www.saucedemo.com");

        driver.findElement(usernameField).sendKeys("locked_out_user");
        driver.findElement(passwordField).sendKeys("secret_sauce");
        driver.findElement(loginButton).click();

        String errorText = driver.findElement(errorMessage).getText();

        // Log the actual error text for visibility in the report.
        ExtentReportManager.getTest().info("Error text received: " + errorText);

        Assert.assertTrue(
                errorText.contains("locked out"),
                "Expected 'locked out' in error message but got: " + errorText
        );
    }
}
