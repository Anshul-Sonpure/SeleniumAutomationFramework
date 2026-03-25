package com.automation.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * WaitUtils — Centralised wait strategies for synchronising test execution
 * with the browser's state.
 *
 * WHY EXPLICIT WAITS INSTEAD OF IMPLICIT WAITS?
 * ────────────────────────────────────────────────
 * • Implicit waits apply globally and poll for element PRESENCE only.
 *   They can mask slow pages by silently waiting — making failures harder to debug.
 *
 * • Explicit waits (WebDriverWait + ExpectedConditions) let you specify EXACTLY
 *   what condition must be met (visible, clickable, text present, etc.) and on
 *   which element. Failures pinpoint the exact condition that timed out.
 *
 * • Mixing implicit + explicit waits can cause unpredictable double-wait behaviour.
 *   This framework uses a small implicit wait (5 s) only as a safety net for
 *   simple finds, and explicit waits for all meaningful synchronisation.
 *
 * WAIT TYPES OVERVIEW:
 * ─────────────────────
 *  WebDriverWait  — polls every 500 ms by default; stops as soon as condition is met.
 *  FluentWait     — like WebDriverWait but lets you customise the poll interval and
 *                   which exceptions to ignore mid-poll.
 *  hardWait       — Thread.sleep() equivalent; use only when all else fails.
 */
public class WaitUtils {

    private static final Logger log = LogManager.getLogger(WaitUtils.class);

    private final WebDriver driver;

    // Timeout and polling interval are read from config so they can be
    // tuned per environment without recompiling.
    private final int timeout;   // seconds — how long to wait before throwing TimeoutException
    private final int polling;   // milliseconds — how often to re-check the condition

    public WaitUtils(WebDriver driver) {
        this.driver  = driver;
        this.timeout = ConfigReader.getInt("explicit.wait", 10);
        this.polling = ConfigReader.getInt("polling.interval", 500);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Visibility waits
    // An element is "visible" when it exists in the DOM AND its CSS display/
    // visibility properties don't hide it (width and height > 0).
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Waits until the element identified by 'locator' is visible.
     * Returns the element so callers can chain: wait.forVisible(loc).getText()
     */
    public WebElement forVisible(By locator) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeout))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Overloaded forVisible for an already-found WebElement.
     * Useful when you have a @FindBy-annotated field from PageFactory.
     */
    public WebElement forVisible(WebElement element) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeout))
                .until(ExpectedConditions.visibilityOf(element));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Clickability waits
    // An element is "clickable" when it is visible AND enabled (not greyed out).
    // Always use this before clicking — a visible but disabled button will throw.
    // ─────────────────────────────────────────────────────────────────────────

    /** Waits until the element located by 'locator' is clickable, then returns it. */
    public WebElement forClickable(By locator) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeout))
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    /** Overloaded forClickable for an already-found WebElement. */
    public WebElement forClickable(WebElement element) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeout))
                .until(ExpectedConditions.elementToBeClickable(element));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Invisibility / absence waits
    // Useful when waiting for a loading spinner or modal to disappear before
    // proceeding with the next action.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Waits until the element is either invisible OR absent from the DOM.
     * Returns true when the condition is met; throws TimeoutException otherwise.
     */
    public boolean forInvisible(By locator) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeout))
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Text / URL condition waits
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Waits until the element's visible text contains the expected string.
     * Handy for asserting dynamic content (e.g. a success banner message).
     */
    public boolean forTextPresent(By locator, String text) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeout))
                .until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    /**
     * Waits until the current URL contains the given fragment.
     * Useful after a redirect — e.g. wait until URL contains "/dashboard".
     */
    public boolean forUrlContains(String fragment) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeout))
                .until(ExpectedConditions.urlContains(fragment));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fluent wait
    // More configurable than WebDriverWait:
    //  • Custom poll interval (not just 500 ms)
    //  • Can ignore specific exception types between polls
    // Use for elements that appear and disappear rapidly (e.g. flashing banners).
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Polls for the element at the configured polling interval, ignoring all
     * exceptions between polls. Stops as soon as the element is found.
     *
     * @param locator  element locator to look for
     * @return         the found WebElement
     */
    public WebElement fluent(By locator) {
        return new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))      // max wait time
                .pollingEvery(Duration.ofMillis(polling))       // check every N ms
                .ignoring(Exception.class)                      // swallow NoSuchElement etc. between polls
                .until(d -> d.findElement(locator));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Page load wait
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Waits until the browser reports that the page has fully loaded by
     * checking document.readyState == "complete" via JavaScript.
     *
     * NOTE: This covers synchronous HTML/JS loading only. For SPA frameworks
     * (React, Angular) that load data asynchronously via AJAX, you still need
     * to wait for specific elements to appear after page load.
     */
    public void forPageLoad() {
        new WebDriverWait(driver, Duration.ofSeconds(timeout))
                .until(d -> ((JavascriptExecutor) d)
                        .executeScript("return document.readyState")
                        .equals("complete"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hard wait — last resort
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Pauses execution for the specified number of seconds regardless of browser state.
     *
     * WARNING: Hard waits make tests slower and brittle — they add fixed delays
     * even when the page is already ready. Only use this when there is genuinely
     * no observable DOM condition to wait on (e.g. a non-DOM animation).
     *
     * @param seconds  how many seconds to sleep
     */
    public void hardWait(int seconds) {
        try {
            Thread.sleep(Duration.ofSeconds(seconds).toMillis());
        } catch (InterruptedException e) {
            // Restore the interrupted status so callers can react if needed.
            Thread.currentThread().interrupt();
            log.warn("Thread interrupted during hard wait");
        }
    }
}
