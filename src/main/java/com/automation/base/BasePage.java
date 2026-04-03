package com.automation.base;

import com.automation.driver.DriverManager;
import com.automation.utils.ConfigReader;
import com.automation.utils.WaitUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

// Foundation for all Page Objects — exposes wrapped Selenium interactions with built-in waits and stale-element retry.
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WaitUtils wait;
    private static final Logger log = LogManager.getLogger(BasePage.class);

    // Retry limit on StaleElementReferenceException before re-throwing.
    private static final int MAX_RETRIES = 2;

    private static final int ACTION_DELAY_MS = ConfigReader.getInt("action.delay", 500);

    protected BasePage() {
        this.driver = DriverManager.getDriver();
        this.wait   = new WaitUtils(driver);
        PageFactory.initElements(driver, this); // wires @FindBy fields to live DOM proxies
    }

    // Retries a void action when the element goes stale mid-interaction.
    private void retryOnStale(Runnable action) {
        StaleElementReferenceException lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                action.run();
                return;
            } catch (StaleElementReferenceException e) {
                lastException = e;
                log.warn("Stale element on attempt {}/{} — retrying...", attempt, MAX_RETRIES);
            }
        }
        log.error("Element still stale after {} attempts", MAX_RETRIES);
        throw lastException;
    }

    // Retries a value-returning action when the element goes stale mid-interaction.
    private <T> T retryOnStale(Supplier<T> action) {
        StaleElementReferenceException lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return action.get();
            } catch (StaleElementReferenceException e) {
                lastException = e;
                log.warn("Stale element on attempt {}/{} — retrying...", attempt, MAX_RETRIES);
            }
        }
        log.error("Element still stale after {} attempts", MAX_RETRIES);
        throw lastException;
    }

    public void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        driver.get(url);
    }

    public String getTitle()      { return driver.getTitle(); }
    public String getCurrentUrl() { return driver.getCurrentUrl(); }

    public void click(By locator) {
        log.debug("Click: {}", locator);
        retryOnStale(() -> wait.forClickable(locator).click());
        actionDelay();
    }

    public void click(WebElement element) {
        log.debug("Click WebElement");
        retryOnStale(() -> wait.forClickable(element).click());
        actionDelay();
    }

    // JS click bypasses overlapping elements that block the normal browser event.
    public void jsClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    public void type(By locator, String text) {
        log.debug("Type value into: {}", locator);
        retryOnStale(() -> {
            WebElement el = wait.forVisible(locator);
            el.clear();
            el.sendKeys(text);
        });
        actionDelay();
    }

    public void type(WebElement element, String text) {
        log.debug("Type value into WebElement");
        retryOnStale(() -> {
            wait.forClickable(element).clear();
            element.sendKeys(text);
        });
        actionDelay();
    }

    public String getText(By locator)      { return retryOnStale(() -> wait.forVisible(locator).getText()); }
    public String getText(WebElement el)   { return retryOnStale(() -> wait.forVisible(el).getText()); }

    public String getAttribute(By locator, String attr) {
        return retryOnStale(() -> wait.forVisible(locator).getAttribute(attr));
    }

    // Returns false instead of throwing when element is absent or stale — safe for conditional checks.
    public boolean isDisplayed(By locator) {
        try { return driver.findElement(locator).isDisplayed(); }
        catch (NoSuchElementException | StaleElementReferenceException e) { return false; }
    }

    public boolean isDisplayed(WebElement element) {
        try { return element.isDisplayed(); }
        catch (NoSuchElementException | StaleElementReferenceException e) { return false; }
    }

    public List<WebElement> findElements(By locator) { return driver.findElements(locator); }

    public void selectByVisibleText(By locator, String text) {
        retryOnStale(() -> new Select(wait.forVisible(locator)).selectByVisibleText(text));
    }

    public void selectByValue(By locator, String value) {
        retryOnStale(() -> new Select(wait.forVisible(locator)).selectByValue(value));
    }

    public void hover(By locator) {
        retryOnStale(() -> new Actions(driver).moveToElement(wait.forVisible(locator)).perform());
    }

    public void scrollToElement(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
    }

    public void waitForPageLoad() { wait.forPageLoad(); }

    // -------------------------------------------------------------------------
    // Native browser dialog (JS alert / confirm / prompt) handling
    // Use these whenever the AUT triggers a window.alert(), confirm(), or prompt().
    // Both methods are safe to call even when no dialog is present — they simply
    // return false without throwing.
    // -------------------------------------------------------------------------

    // Accepts (clicks OK on) a JS alert/confirm dialog if one is present.
    // Returns true if a dialog was found and accepted, false if none appeared.
    public boolean acceptAlertIfPresent() {
        try {
            Alert alert = new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.alertIsPresent());
            log.info("Alert detected: '{}' — accepting", alert.getText());
            alert.accept();
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    // Dismisses (clicks Cancel on) a JS confirm/prompt dialog if one is present.
    // For plain alert() dialogs there is no Cancel — use acceptAlertIfPresent() instead.
    // Returns true if a dialog was found and dismissed, false if none appeared.
    public boolean dismissAlertIfPresent() {
        try {
            Alert alert = new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.alertIsPresent());
            log.info("Alert detected: '{}' — dismissing", alert.getText());
            alert.dismiss();
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private void actionDelay() {
        if (ACTION_DELAY_MS > 0) {
            try { Thread.sleep(ACTION_DELAY_MS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
}
