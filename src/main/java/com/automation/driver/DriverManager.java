package com.automation.driver;

import org.openqa.selenium.WebDriver;

/**
 * DriverManager — Central registry for WebDriver instances.
 *
 * WHY ThreadLocal?
 * ─────────────────
 * When tests run in parallel (e.g. parallel="methods" in testng.xml), multiple
 * threads execute simultaneously. If we stored the WebDriver in a plain static
 * variable, Thread A could accidentally use Thread B's browser — causing
 * unpredictable failures and race conditions.
 *
 * ThreadLocal<T> gives each thread its own isolated copy of the variable.
 * Thread A's getDriver() always returns Thread A's WebDriver, and Thread B's
 * getDriver() always returns Thread B's WebDriver — completely independent.
 *
 * Usage flow per test thread:
 *   1. BaseTest.setUp()    → DriverFactory creates driver → DriverManager.setDriver()
 *   2. Test method runs    → page objects call DriverManager.getDriver()
 *   3. BaseTest.tearDown() → driver.quit()               → DriverManager.removeDriver()
 */
public class DriverManager {

    /**
     * ThreadLocal container that holds one WebDriver per thread.
     * Declared private so only this class can mutate it — all access
     * goes through the controlled get/set/remove API below.
     */
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    // Private constructor prevents instantiation — this class is a pure utility.
    private DriverManager() {}

    /**
     * Returns the WebDriver bound to the currently running thread.
     * Returns null if setDriver() has not been called yet for this thread.
     */
    public static WebDriver getDriver() {
        return driverThreadLocal.get();
    }

    /**
     * Binds a WebDriver to the currently running thread.
     * Called once per test in BaseTest.setUp() after the driver is created.
     *
     * @param driver the fully initialised WebDriver instance
     */
    public static void setDriver(WebDriver driver) {
        driverThreadLocal.set(driver);
    }

    /**
     * Removes the WebDriver reference from the current thread's ThreadLocal slot.
     *
     * IMPORTANT: Always call this after driver.quit() in tearDown().
     * Failing to remove causes a memory leak because ThreadLocal entries
     * are held by the thread itself — if the thread is reused (e.g. in a
     * thread pool), the old driver reference lingers in memory indefinitely.
     */
    public static void removeDriver() {
        driverThreadLocal.remove();
    }
}
