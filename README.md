# Selenium UI Automation Framework

A production-ready Java UI test automation framework built on **Selenium 4**, **TestNG**, and the **Page Object Model (POM)**. Targets [saucedemo.com](https://www.saucedemo.com) and demonstrates end-to-end test coverage with parallel execution support, rich HTML reporting, animated GIF screen recording, and full CI/CD readiness.

---

## Tech Stack

| Tool / Library | Version | Purpose |
|---|---|---|
| Java | 11 | Language |
| Selenium WebDriver | 4.33.0 | Browser automation |
| TestNG | 7.9.0 | Test runner & assertions |
| WebDriverManager | 6.1.0 | Automatic driver binary management |
| ExtentReports | 5.1.1 | HTML test reporting |
| Log4j2 | 2.23.0 | Structured logging |
| animated-gif-lib | 1.4 | Animated GIF screen recording |
| Apache Commons IO | 2.15.1 | File utilities |
| Maven | 3.x | Build & dependency management |

---

## Folder Structure

```
selenium-ui-framework/
│
├── pom.xml                                         # Maven build + all dependencies
│
├── src/
│   ├── main/java/com/automation/
│   │   ├── base/
│   │   │   └── BasePage.java                       # Parent for all Page Objects; wraps Selenium interactions with waits, stale-element retry, action delay, and alert handling
│   │   │
│   │   ├── driver/
│   │   │   ├── DriverFactory.java                  # Creates browser-specific WebDriver instances (Chrome / Firefox / Edge) with options, incognito, headless support
│   │   │   └── DriverManager.java                  # ThreadLocal<WebDriver> registry — parallel-safe get/set/remove
│   │   │
│   │   ├── listeners/
│   │   │   └── TestListener.java                   # Bridges TestNG events to ExtentReports & Log4j2; starts/stops VideoRecorder; embeds GIF in report
│   │   │
│   │   ├── pages/
│   │   │   ├── LoginPage.java                      # Login screen — enterUsername, enterPassword, login(), getErrorMessage()
│   │   │   ├── ProductsPage.java                   # Product listing — addToCartByName(), getCartCount(), clickCart()
│   │   │   ├── CartPage.java                       # Shopping cart — verifyItem(), clickCheckout()
│   │   │   ├── CheckoutInfoPage.java               # Checkout step 1 — fillInfo(), clickContinue()
│   │   │   ├── CheckoutOverviewPage.java           # Checkout step 2 — getOrderTotal(), clickFinish()
│   │   │   └── OrderConfirmationPage.java          # Confirmation screen — getConfirmationMessage(), saves order details to file
│   │   │
│   │   └── utils/
│   │       ├── ConfigReader.java                   # Loads config.properties; System property overrides always win
│   │       ├── ExtentReportManager.java            # Singleton ExtentReports instance; ThreadLocal<ExtentTest> for parallel safety
│   │       ├── ScreenshotUtils.java                # Captures screenshots as file or Base64 bytes
│   │       ├── VideoRecorder.java                  # Per-thread animated GIF recorder using Robot + ScheduledExecutorService
│   │       └── WaitUtils.java                      # Centralised explicit / fluent waits wrapping ExpectedConditions
│   │
│   └── test/
│       ├── java/com/automation/
│       │   ├── base/
│       │   │   └── BaseTest.java                   # Parent for all test classes; manages driver lifecycle + ExtentReport init/flush
│       │   │
│       │   └── tests/
│       │       ├── LoginTest.java                  # Login scenarios: valid, invalid, locked-out, empty credentials
│       │       └── CheckoutFlowTest.java           # E2E checkout: login → add items → cart → checkout → confirm order
│       │
│       └── resources/
│           ├── config.properties                   # All runtime configuration (browser, timeouts, flags)
│           ├── testng.xml                          # TestNG suite definition
│           └── log4j2.xml                         # Log4j2 appender / rolling-file configuration
│
└── test-output/
    ├── reports/
    │   └── ExtentReport_<timestamp>.html           # Self-contained HTML test report (generated at runtime)
    ├── screenshots/
    │   └── <testName>_<timestamp>.png              # Failure screenshots (generated at runtime)
    └── videos/
        └── <testName>_<timestamp>.gif              # Animated GIF recordings per test (generated at runtime)
```

---

## Architecture

### Layer Overview

```
Tests  →  Page Objects  →  BasePage  →  WaitUtils  →  WebDriver
  ↓                                                        ↑
BaseTest  →  DriverFactory  →  DriverManager (ThreadLocal)
  ↓
TestListener  →  ExtentReportManager + VideoRecorder
```

### Driver Management
`DriverFactory` creates a configured `WebDriver` for the requested browser. `DriverManager` stores it in a `ThreadLocal<WebDriver>`, ensuring every parallel thread gets its own isolated driver instance. Always use `DriverManager.getDriver()` — never store driver references in fields.

### Page Object Model
Every screen is a class in `com.automation.pages` that extends `BasePage`. Locators are declared as `@FindBy` fields (lazy PageFactory proxies). Interaction methods (`click`, `type`, `getText`) are inherited from `BasePage`, which adds built-in waits and stale-element retry automatically.

### Waits
All waits are centralised in `WaitUtils`. Use `forVisible()`, `forClickable()`, `fluent()`, etc. Never use raw `Thread.sleep` in test or page code — use `WaitUtils.hardWait()` only as a last resort.

### Reporting
`ExtentReportManager` is a double-checked locking singleton that writes `test-output/reports/ExtentReport_<timestamp>.html`. `TestListener` wires every TestNG event to both Log4j2 and ExtentReport, embedding a failure screenshot and the animated GIF recording for every test.

### Video Recording
`VideoRecorder` starts a `ScheduledExecutorService` in `onTestStart` that captures the full screen at the configured FPS using `java.awt.Robot`. On test end it encodes the frames into an animated GIF (scaled to 50% for file size) and embeds it inline in the ExtentReport via a relative `<img>` tag.

---

## Configuration

All properties live in `src/test/resources/config.properties`. Every property can be overridden at runtime with a `-D` flag.

| Property | Default | Override example | Description |
|---|---|---|---|
| `browser` | `chrome` | `-Dbrowser=firefox` | Browser to use: `chrome`, `firefox`, `edge` |
| `headless` | `false` | `-Dheadless=true` | Run without a visible browser window (CI mode) |
| `incognito` | `true` | `-Dincognito=false` | Run in private/incognito mode |
| `base.url` | `https://www.saucedemo.com` | | URL opened before each test |
| `implicit.wait` | `5` | | Seconds WebDriver polls for elements |
| `explicit.wait` | `10` | | Seconds for explicit condition waits |
| `page.load.timeout` | `30` | | Max seconds to wait for page load |
| `polling.interval` | `500` | | Milliseconds between FluentWait polls |
| `action.delay` | `500` | | Milliseconds pause after each click/type (visible pacing for recordings) |
| `video.enabled` | `true` | `-Dvideo.enabled=false` | Enable/disable GIF recording |
| `video.fps` | `5` | | Frames per second for GIF capture |
| `environment` | `QA` | | Label shown in the ExtentReport system info |

---

## Running Tests

### Prerequisites
- Java 11+
- Maven 3.6+
- Chrome / Firefox / Edge installed (drivers are downloaded automatically by WebDriverManager)

### Commands

```bash
# Run full suite (default: Chrome, incognito, 500ms action delay)
mvn clean test

# Run with a different browser
mvn test -Dbrowser=firefox
mvn test -Dbrowser=edge

# Run headless (CI/CD)
mvn test -Dheadless=true

# Run in normal (non-incognito) window
mvn test -Dincognito=false

# Disable screen recording (faster, no GIF output)
mvn test -Dvideo.enabled=false

# Run a specific TestNG group
mvn test -Dgroups=smoke

# Combine flags
mvn test -Dbrowser=firefox -Dheadless=true -Dvideo.enabled=false
```

### Test Suite
The active suite is defined in `src/test/resources/testng.xml`. To add a new test class, register it there:

```xml
<test name="Login Tests">
    <classes>
        <class name="com.automation.tests.LoginTest"/>
    </classes>
</test>
```

---

## Test Coverage

### LoginTest
| Test | Description |
|---|---|
| `testSuccessfulLogin` | Valid credentials navigate to the Products page |
| `testInvalidLogin` | Invalid credentials display an error banner |
| `testLockedOutUser` | Locked-out user account shows a specific error |
| `testEmptyCredentials` | Login attempt with empty fields shows a validation error |

### CheckoutFlowTest
| Test | Description |
|---|---|
| `testCompleteCheckoutFlow` | End-to-end: login → add 2 items to cart → verify cart → fill checkout info → confirm order total → place order → verify confirmation message |

---

## Adding a New Page Object

1. Create a class in `src/main/java/com/automation/pages/` extending `BasePage`.
2. Declare element locators as `@FindBy` fields.
3. Expose user-action methods that use the inherited `click()`, `type()`, `getText()`, etc.

```java
public class ExamplePage extends BasePage {

    @FindBy(id = "submit-btn")
    private WebElement submitButton;

    public ExamplePage() { super(); }

    public void submit() {
        click(submitButton);
    }
}
```

## Adding a New Test Class

1. Create a class in `src/test/java/com/automation/tests/` extending `BaseTest`.
2. Instantiate page objects in a `@BeforeMethod` (driver is available by then).
3. Register the class in `testng.xml`.

```java
public class ExampleTest extends BaseTest {

    private ExamplePage page;

    @BeforeMethod
    public void initPage() { page = new ExamplePage(); }

    @Test(description = "Example test")
    public void testSomething() {
        Assert.assertTrue(page.isLoaded());
    }
}
```

---

## Reports & Artifacts

After a test run, the following are generated under `test-output/`:

| Artifact | Location | Description |
|---|---|---|
| HTML Report | `test-output/reports/ExtentReport_<ts>.html` | Full test results with logs, screenshots, and GIF recordings |
| Screenshots | `test-output/screenshots/<name>_<ts>.png` | Captured automatically on test failure |
| GIF Recordings | `test-output/videos/<name>_<ts>.gif` | Animated screen recording of every test run |
| Log file | `logs/automation.log` | Rolling daily log (10 rotations, 10 MB cap) |

---

## CI/CD

For pipelines with no display server (e.g. GitHub Actions, Jenkins):

```bash
mvn clean test -Dheadless=true -Dvideo.enabled=false
```

`VideoRecorder` automatically disables itself when `headless=true` or when `GraphicsEnvironment.isHeadless()` returns true — no configuration change needed in CI environments.
