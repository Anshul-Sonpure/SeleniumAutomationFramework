package com.automation.base;

import com.automation.driver.DriverManager;
import com.automation.utils.WaitUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * BasePage — Foundation class for the Page Object Model (POM).
 *
 * PAGE OBJECT MODEL:
 * ───────────────────
 * Each web page (or major component) in the application under test gets its own
 * Java class. That class stores the element locators and exposes methods that
 * describe actions a user can take on that page (e.g. login(), searchProduct()).
 *
 * Benefits:
 *  • Test code is decoupled from locators — if a locator changes you fix it in
 *    one place rather than across dozens of test files.
 *  • Methods model user intent, making tests readable (loginPage.login("user","pass")).
 *  • Inheritance from BasePage gives every page object access to common helpers
 *    (click, type, getText…) without code duplication.
 *
 * WHY ABSTRACT?
 * ──────────────
 * BasePage itself is not a real page — it is a blueprint. Making it abstract
 * prevents instantiating it directly and signals that every concrete page class
 * must extend it.
 */
public abstract class BasePage {

    // driver: shared across all page objects in the same thread.
    // Fetched from DriverManager (ThreadLocal) so it is always the correct
    // driver for the currently running test thread.
    protected final WebDriver driver;

    // wait: provides explicit waits for elements — avoids hardcoded Thread.sleep().
    protected final WaitUtils wait;

    private static final Logger log = LogManager.getLogger(BasePage.class);

    /**
     * Constructor called by every concrete Page Object via super().
     *
     * PageFactory.initElements scans the subclass for fields annotated with
     * @FindBy / @FindAll and wraps them in lazy proxies. The actual
     * driver.findElement() call is deferred until the field is first accessed —
     * this is called "lazy initialisation" and avoids stale element exceptions
     * that would occur if elements were located at construction time.
     */
    protected BasePage() {
        this.driver = DriverManager.getDriver();
        this.wait   = new WaitUtils(driver);
        PageFactory.initElements(driver, this);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Opens the given URL in the current browser tab.
     * Prefer calling this over driver.get() directly so navigation is logged.
     */
    public void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        driver.get(url);
    }

    /** Returns the text inside the browser's title bar / tab. */
    public String getTitle() {
        return driver.getTitle();
    }

    /** Returns the full URL currently shown in the address bar. */
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Click helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Waits for the element identified by 'locator' to become clickable,
     * then clicks it. Uses WaitUtils.forClickable() internally which applies
     * an explicit wait — far more reliable than a plain driver.findElement().click().
     */
    public void click(By locator) {
        log.debug("Click: {}", locator);
        wait.forClickable(locator).click();
    }

    /**
     * Overloaded click for an already-found WebElement.
     * Still waits for clickability to guard against elements that are
     * visible but temporarily disabled (e.g. a submit button before form loads).
     */
    public void click(WebElement element) {
        log.debug("Click WebElement");
        wait.forClickable(element).click();
    }

    /**
     * Performs a click via JavaScript — bypasses the normal browser event chain.
     *
     * USE CASE: Sometimes an overlay or another element sits on top of the target
     * element and intercepts the regular click, throwing ElementClickInterceptedException.
     * A JS click goes directly to the element's DOM node, bypassing the interception.
     *
     * NOTE: JS click does NOT mimic real user behaviour (no hover, no focus),
     * so prefer the regular click() and only fall back to this when necessary.
     */
    public void jsClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Text input helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Clears any existing text in the input field, then types the given text.
     * Waits for the element to be visible before interacting.
     */
    public void type(By locator, String text) {
        log.debug("Type '{}' into: {}", text, locator);
        WebElement el = wait.forVisible(locator);
        el.clear();           // Remove pre-filled / previously typed text
        el.sendKeys(text);    // Simulate keystroke-by-keystroke typing
    }

    /**
     * Overloaded type for an already-found WebElement.
     * Waits for clickability (stronger than visibility — also ensures it's enabled).
     */
    public void type(WebElement element, String text) {
        log.debug("Type '{}' into WebElement", text);
        wait.forClickable(element).clear();
        element.sendKeys(text);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Text / attribute retrieval
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the visible inner text of an element.
     * Waits for the element to be visible first (invisible elements return "").
     */
    public String getText(By locator) {
        return wait.forVisible(locator).getText();
    }

    /** Overloaded getText for an already-found WebElement. */
    public String getText(WebElement element) {
        return wait.forVisible(element).getText();
    }

    /**
     * Returns the value of an HTML attribute on the element.
     * Example: getAttribute(By.id("logo"), "src") returns the image URL.
     */
    public String getAttribute(By locator, String attribute) {
        return wait.forVisible(locator).getAttribute(attribute);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Visibility / presence checks
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the element is present in the DOM and visible on the page.
     * Returns false (instead of throwing) when the element is absent — useful for
     * conditional logic like "if error banner is displayed, log it".
     */
    public boolean isDisplayed(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (NoSuchElementException e) {
            // Element not found in the DOM — treat as "not displayed"
            return false;
        }
    }

    /**
     * Returns all elements matching the given locator.
     * Returns an empty list (not an exception) when no elements match.
     */
    public List<WebElement> findElements(By locator) {
        return driver.findElements(locator);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dropdown helpers  (works with native HTML <select> elements)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Selects a <option> in a <select> dropdown by its visible label text.
     * Example: selectByVisibleText(By.id("country"), "India")
     */
    public void selectByVisibleText(By locator, String text) {
        new Select(wait.forVisible(locator)).selectByVisibleText(text);
    }

    /**
     * Selects a <option> by its 'value' HTML attribute.
     * Example: <option value="IN">India</option>  →  selectByValue(locator, "IN")
     */
    public void selectByValue(By locator, String value) {
        new Select(wait.forVisible(locator)).selectByValue(value);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mouse / keyboard interactions
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Moves the mouse cursor over the element without clicking it.
     * Triggers CSS :hover states and reveals dropdown menus that only appear
     * on hover in the application.
     */
    public void hover(By locator) {
        new Actions(driver).moveToElement(wait.forVisible(locator)).perform();
    }

    /**
     * Scrolls the page so the element is visible in the viewport.
     * Needed before interacting with elements that are off-screen (e.g. at
     * the bottom of a long page), as Selenium can't click invisible elements.
     */
    public void scrollToElement(WebElement element) {
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView(true);", element);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Page-level state
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Blocks until document.readyState == "complete", meaning the HTML and
     * synchronous JavaScript have fully loaded. Does NOT wait for async AJAX.
     * Call this after navigations or form submissions if elements don't appear.
     */
    public void waitForPageLoad() {
        wait.forPageLoad();
    }
}
