package com.automation.pages;

import com.automation.base.BasePage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * LoginPage — Page Object for the login screen of the application under test.
 *
 * PAGE OBJECT MODEL PRINCIPLES APPLIED HERE:
 * -------------------------------------------
 * 1. Locators are declared as @FindBy fields — not hardcoded in test methods.
 *    If the "user-name" id changes to "username", you fix it in this ONE place.
 *
 * 2. Methods describe WHAT a user does ("enterUsername", "clickLoginButton"),
 *    not HOW Selenium does it. Tests read like plain English as a result.
 *
 * 3. Actions that navigate to a new page return the NEXT page object.
 *    login() returns a ProductsPage — the test gets back the page it lands on,
 *    making the flow self-documenting:
 *      ProductsPage page = loginPage.login("user", "pass");
 *      Assert.assertEquals(page.getPageTitle(), "Products");
 *
 * @FindBy PROXIES AND STALE ELEMENTS:
 * ------------------------------------
 * PageFactory (called in BasePage constructor) wraps every @FindBy field in a
 * dynamic proxy. The proxy calls driver.findElement() FRESH each time the field
 * is accessed — not at construction time. This means:
 *   - If the DOM refreshes between two accesses, the second access gets a brand
 *     new element reference pointing to the current DOM node.
 *   - Combined with BasePage.retryOnStale(), most stale element scenarios are
 *     handled without any extra code in the page class.
 */
public class LoginPage extends BasePage {

    // @FindBy is PageFactory's annotation for declaring element locators.
    // It is equivalent to driver.findElement(By.id("user-name")) but lazily evaluated.

    /** Username input field — identified by its HTML id attribute. */
    @FindBy(id = "user-name")
    private WebElement usernameField;

    /** Password input field. */
    @FindBy(id = "password")
    private WebElement passwordField;

    /** The Login submit button. */
    @FindBy(id = "login-button")
    private WebElement loginButton;

    /**
     * Error banner shown on failed login.
     * Using data-test attribute — these are the most stable locators because
     * they are specifically added for automation and rarely change with UI redesigns.
     */
    @FindBy(css = "[data-test='error']")
    private WebElement errorMessage;

    /**
     * Constructor — calls BasePage() which runs PageFactory.initElements(driver, this).
     * This registers all @FindBy fields in this class as lazy proxies.
     */
    public LoginPage() {
        super();
    }

    // -------------------------------------------------------------------------
    // Individual user actions
    // Keep each method focused on one thing so tests can compose them flexibly.
    // -------------------------------------------------------------------------

    /**
     * Types the given username into the username field.
     * Delegates to BasePage.type() which handles wait + stale retry internally.
     *
     * @param username  the username string to enter
     */
    public void enterUsername(String username) {
        type(usernameField, username);
    }

    /**
     * Types the given password into the password field.
     *
     * @param password  the password string to enter
     */
    public void enterPassword(String password) {
        type(passwordField, password);
    }

    /**
     * Clicks the Login button.
     * Delegates to BasePage.click() which waits for clickability + handles stale retry.
     */
    public void clickLoginButton() {
        click(loginButton);
    }

    // -------------------------------------------------------------------------
    // Composed actions (multiple steps in one call)
    // -------------------------------------------------------------------------

    /**
     * Performs a full login sequence: enter credentials and click Login.
     *
     * Returns a ProductsPage because a successful login always navigates
     * to the Products page. Returning the next page object allows tests
     * to chain naturally without needing a separate page instantiation step.
     *
     * NOTE: This method assumes valid credentials. For invalid credentials,
     * use enterUsername() + enterPassword() + clickLoginButton() individually
     * so you can assert on the error message before navigating away.
     *
     * @param username  valid username
     * @param password  valid password
     * @return          ProductsPage object representing the landing page
     */
    public ProductsPage login(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLoginButton();
        // Return the next page — the caller receives a ready-to-use page object
        return new ProductsPage();
    }

    // -------------------------------------------------------------------------
    // Assertions / state readers
    // -------------------------------------------------------------------------

    /**
     * Returns the full text of the error message banner.
     * Delegates to BasePage.getText() which waits for visibility + handles stale retry.
     *
     * @return  error message string shown to the user (e.g. "Epic sadface: ...")
     */
    public String getErrorMessage() {
        return getText(errorMessage);
    }

    /**
     * Returns true if the error banner is currently visible on the page.
     * Delegates to BasePage.isDisplayed(WebElement) which catches both
     * NoSuchElementException and StaleElementReferenceException — so this
     * method never throws; it always returns true or false.
     *
     * @return  true if error banner is displayed, false otherwise
     */
    public boolean isErrorMessageDisplayed() {
        return isDisplayed(errorMessage);
    }
}
