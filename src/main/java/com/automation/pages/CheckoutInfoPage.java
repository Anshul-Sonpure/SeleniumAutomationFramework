package com.automation.pages;

import com.automation.base.BasePage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * CheckoutInfoPage — Page Object for the checkout information form (/checkout-step-one.html).
 *
 * This is the first step of the checkout flow. The user must provide their
 * first name, last name, and postal/zip code before the order can proceed.
 */
public class CheckoutInfoPage extends BasePage {

    private static final Logger log = LogManager.getLogger(CheckoutInfoPage.class);

    /** First name input field. Identified by data-test attribute for stability. */
    @FindBy(css = "[data-test='firstName']")
    private WebElement firstNameField;

    /** Last name input field. */
    @FindBy(css = "[data-test='lastName']")
    private WebElement lastNameField;

    /** Postal / zip code input field. */
    @FindBy(css = "[data-test='postalCode']")
    private WebElement zipCodeField;

    /**
     * "Continue" button — submits the form and moves to the order overview page.
     * Only enabled after all required fields are filled in.
     */
    @FindBy(css = "[data-test='continue']")
    private WebElement continueButton;

    /** Error message banner shown if required fields are left empty. */
    @FindBy(css = "[data-test='error']")
    private WebElement errorMessage;

    public CheckoutInfoPage() {
        super();
    }

    // -------------------------------------------------------------------------
    // Individual field actions
    // -------------------------------------------------------------------------

    /**
     * Types the first name into the First Name field.
     * Delegates to BasePage.type(WebElement, String) which applies wait + stale retry.
     *
     * @param firstName  buyer's first name
     */
    public void enterFirstName(String firstName) {
        type(firstNameField, firstName);
    }

    /**
     * Types the last name into the Last Name field.
     *
     * @param lastName  buyer's last name
     */
    public void enterLastName(String lastName) {
        type(lastNameField, lastName);
    }

    /**
     * Types the postal/zip code into the Zip Code field.
     *
     * @param zipCode  buyer's zip/postal code
     */
    public void enterZipCode(String zipCode) {
        type(zipCodeField, zipCode);
    }

    // -------------------------------------------------------------------------
    // Composed action
    // -------------------------------------------------------------------------

    /**
     * Fills all three required fields and clicks Continue in a single call.
     * Returns the CheckoutOverviewPage that the user lands on after submitting.
     *
     * This is the primary method tests should use — it expresses the full
     * user intent ("fill in my details and move on") without exposing the
     * three individual field steps to test code.
     *
     * @param firstName  buyer's first name
     * @param lastName   buyer's last name
     * @param zipCode    buyer's zip/postal code
     * @return           CheckoutOverviewPage — the order review screen
     */
    public CheckoutOverviewPage fillAndContinue(String firstName, String lastName, String zipCode) {
        enterFirstName(firstName);
        enterLastName(lastName);
        enterZipCode(zipCode);
        log.info("Checkout info filled — Name: {} {}, Zip: {}", firstName, lastName, zipCode);
        click(continueButton);
        return new CheckoutOverviewPage();
    }

    // -------------------------------------------------------------------------
    // Validation state
    // -------------------------------------------------------------------------

    /** Returns true if the error banner is visible (e.g. missing required field). */
    public boolean isErrorDisplayed() {
        return isDisplayed(errorMessage);
    }

    /** Returns the error message text shown when form validation fails. */
    public String getErrorMessage() {
        return getText(errorMessage);
    }
}
