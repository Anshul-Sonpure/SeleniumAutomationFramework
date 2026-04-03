package com.automation.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

// Centralised wait strategies. By-locator overloads ignore StaleElementReferenceException
// so DOM updates during polling don't abort the wait prematurely.
public class WaitUtils {

    private static final Logger log = LogManager.getLogger(WaitUtils.class);

    private final WebDriver driver;
    private final int timeout;
    private final int polling;

    public WaitUtils(WebDriver driver) {
        this.driver  = driver;
        this.timeout = ConfigReader.getInt("explicit.wait", 10);
        this.polling = ConfigReader.getInt("polling.interval", 500);
    }

    public WebElement forVisible(By locator) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeout))
                .ignoring(StaleElementReferenceException.class)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    // WebElement overload — stale handling delegated to BasePage.retryOnStale().
    public WebElement forVisible(WebElement element) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeout))
                .until(ExpectedConditions.visibilityOf(element));
    }

    public WebElement forClickable(By locator) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeout))
                .ignoring(StaleElementReferenceException.class)
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    public WebElement forClickable(WebElement element) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeout))
                .until(ExpectedConditions.elementToBeClickable(element));
    }

    public boolean forInvisible(By locator) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeout))
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public boolean forTextPresent(By locator, String text) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeout))
                .ignoring(StaleElementReferenceException.class)
                .until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    public boolean forUrlContains(String fragment) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeout))
                .until(ExpectedConditions.urlContains(fragment));
    }

    // FluentWait with custom polling interval; ignores transient DOM conditions.
    public WebElement fluent(By locator) {
        return new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(polling))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class)
                .until(d -> d.findElement(locator));
    }

    public void forPageLoad() {
        new WebDriverWait(driver, Duration.ofSeconds(timeout))
                .until(d -> ((JavascriptExecutor) d)
                        .executeScript("return document.readyState")
                        .equals("complete"));
    }

    // Hard wait — last resort when no DOM condition exists to poll on.
    public void hardWait(int seconds) {
        try {
            Thread.sleep(Duration.ofSeconds(seconds).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread interrupted during hard wait");
        }
    }
}
