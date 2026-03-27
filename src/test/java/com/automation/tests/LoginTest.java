package com.automation.tests;

import com.automation.base.BaseTest;
import com.automation.pages.LoginPage;
import com.automation.pages.ProductsPage;
import com.automation.utils.ExtentReportManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * LoginTest — Test class for the login functionality.
 *
 * PROPER PAGE OBJECT MODEL USAGE:
 * ---------------------------------
 * Unlike SampleLoginTest (which used raw By locators directly in test methods),
 * this class interacts with the application ONLY through page objects.
 *
 * What this achieves:
 *   - No locators in test code — tests don't know/care how elements are found.
 *   - If the login button's id changes, only LoginPage.java needs updating,
 *     not every test that clicks the login button.
 *   - Tests read like user stories: "enter username, enter password, click login".
 *
 * EACH TEST IS INDEPENDENT:
 * --------------------------
 * BaseTest.setUp() opens a fresh browser and navigates to base.url before
 * each @Test method. Tests do not share state — one test's actions cannot
 * affect another's. The @BeforeMethod here only instantiates the page object
 * (which is lightweight — no browser interaction happens at construction time).
 */
public class LoginTest extends BaseTest {

    // Page object instance — initialised fresh before each test method.
    // Declared as a field so all @Test methods in this class can access it.
    private LoginPage loginPage;

    /**
     * Runs after BaseTest.setUp() (which opens the browser and navigates to base.url)
     * and before each @Test method. Creates a fresh LoginPage instance.
     *
     * Why create the page object here and not in each test?
     *   - Avoids repeating "loginPage = new LoginPage()" in every @Test method.
     *   - Still gives each test its own instance (no shared mutable state).
     *
     * Why not in the constructor?
     *   - The WebDriver doesn't exist yet when the constructor runs.
     *     It is only available after BaseTest.setUp() has called DriverFactory.
     */
    @BeforeMethod
    public void initPage() {
        loginPage = new LoginPage();
    }

    // -------------------------------------------------------------------------
    // Test cases
    // -------------------------------------------------------------------------

    /**
     * HAPPY PATH: Valid credentials should land the user on the Products page.
     *
     * Uses LoginPage.login() which performs the full login sequence and returns
     * a ProductsPage — the test immediately asserts on the returned page state.
     * No raw locators, no driver references, no By constants in sight.
     */
    @Test(description = "Valid credentials should navigate to the Products page",
          groups = {"smoke"})
    public void testSuccessfulLogin() {
        // login() encapsulates: type username → type password → click login button
        // It returns ProductsPage because that is where a successful login lands.
        ProductsPage productsPage = loginPage.login("standard_user", "secret_sauce");

        String title = productsPage.getPageTitle();
        ExtentReportManager.getTest().info("Page title after login: " + title);

        Assert.assertEquals(title, "Products",
                "Expected to land on the Products page after valid login");
    }

    /**
     * NEGATIVE PATH: Invalid credentials should display an error message.
     *
     * We use the individual step methods (enterUsername + enterPassword + clickLoginButton)
     * instead of login() because we don't want to return a ProductsPage —
     * a failed login keeps the user on the LoginPage where we can assert the error.
     */
    @Test(description = "Invalid credentials should display an error banner")
    public void testInvalidLogin() {
        loginPage.enterUsername("wrong_user");
        loginPage.enterPassword("wrong_pass");
        loginPage.clickLoginButton();

        // isErrorMessageDisplayed() uses BasePage.isDisplayed(WebElement) which
        // catches StaleElementReferenceException and returns false instead of throwing.
        Assert.assertTrue(loginPage.isErrorMessageDisplayed(),
                "Error banner should be visible after invalid login attempt");
    }

    /**
     * EDGE CASE: A known locked-out user account should display a specific error.
     *
     * We assert on partial text (contains "locked out") rather than the exact full
     * message — this makes the test resilient to minor copy/punctuation changes
     * while still verifying the key information the user would see.
     */
    @Test(description = "Locked-out user account should show a 'locked out' error message")
    public void testLockedOutUser() {
        loginPage.enterUsername("locked_out_user");
        loginPage.enterPassword("secret_sauce");
        loginPage.clickLoginButton();

        String errorText = loginPage.getErrorMessage();
        ExtentReportManager.getTest().info("Error message received: " + errorText);

        Assert.assertTrue(errorText.contains("locked out"),
                "Expected 'locked out' in error message but got: [" + errorText + "]");
    }

    /**
     * VALIDATION: Empty credentials should display an error message.
     *
     * Clicking login without entering anything should trigger a validation error.
     * We only assert the error is shown — the exact text is tested in testInvalidLogin.
     */
    @Test(description = "Clicking login with empty fields should show a validation error")
    public void testEmptyCredentials() {
        // No username or password entered — click login directly
        loginPage.clickLoginButton();

        Assert.assertTrue(loginPage.isErrorMessageDisplayed(),
                "Validation error should appear when login is attempted with empty fields");
    }
}
