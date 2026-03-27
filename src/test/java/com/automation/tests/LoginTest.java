package com.automation.tests;

import com.automation.base.BaseTest;
import com.automation.pages.LoginPage;
import com.automation.pages.ProductsPage;
import com.automation.utils.ExtentReportManager;
import org.testng.Assert;
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

    // No instance fields for page objects — with parallel="methods" all test methods
    // share the same class instance. A shared field would create a race condition where
    // threads overwrite each other's page object. Page objects are created locally
    // inside each @Test method instead (same pattern as CheckoutFlowTest).

    @Test(description = "Valid credentials should navigate to the Products page",
          groups = {"smoke", "login"})
    public void testSuccessfulLogin() {
        LoginPage loginPage = new LoginPage();
        ProductsPage productsPage = loginPage.login("standard_user", "secret_sauce");

        String title = productsPage.getPageTitle();
        ExtentReportManager.getTest().info("Page title after login: " + title);

        Assert.assertEquals(title, "Products",
                "Expected to land on the Products page after valid login");
    }

    @Test(description = "Invalid credentials should display an error banner",
          groups = {"login"})
    public void testInvalidLogin() {
        LoginPage loginPage = new LoginPage();
        loginPage.enterUsername("wrong_user");
        loginPage.enterPassword("wrong_pass");
        loginPage.clickLoginButton();

        Assert.assertTrue(loginPage.isErrorMessageDisplayed(),
                "Error banner should be visible after invalid login attempt");
    }

    @Test(description = "Locked-out user account should show a 'locked out' error message",
          groups = {"login"})
    public void testLockedOutUser() {
        LoginPage loginPage = new LoginPage();
        loginPage.enterUsername("locked_out_user");
        loginPage.enterPassword("secret_sauce");
        loginPage.clickLoginButton();

        String errorText = loginPage.getErrorMessage();
        ExtentReportManager.getTest().info("Error message received: " + errorText);

        Assert.assertTrue(errorText.contains("locked out"),
                "Expected 'locked out' in error message but got: [" + errorText + "]");
    }

    @Test(description = "Clicking login with empty fields should show a validation error",
          groups = {"login"})
    public void testEmptyCredentials() {
        LoginPage loginPage = new LoginPage();
        loginPage.clickLoginButton();

        Assert.assertTrue(loginPage.isErrorMessageDisplayed(),
                "Validation error should appear when login is attempted with empty fields");
    }
}
