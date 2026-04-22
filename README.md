# Selenium UI & API Automation Framework

A production-ready Java test automation framework built on **Selenium 4**, **REST Assured**, **TestNG**, and the **Page Object Model (POM)**. Covers both UI testing against [saucedemo.com](https://www.saucedemo.com) and REST API testing against [JSONPlaceholder](https://jsonplaceholder.typicode.com), with parallel execution, rich HTML reporting, animated GIF screen recording, and full CI/CD readiness.

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
| REST Assured | 5.4.0 | REST API testing DSL |
| Jackson Databind | 2.17.1 | JSON serialisation / deserialisation |
| OWASP ZAP Client API | 1.14.0 | ZAP passive scan integration |
| OWASP Dependency-Check | 10.0.4 | CVE scan of Maven dependencies |
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
│   │   ├── api/
│   │   │   ├── BaseApiClient.java                  # Shared RequestSpecification — base URI, JSON content-type, request/response logging filters
│   │   │   ├── endpoints/
│   │   │   │   ├── PostsEndpoint.java              # /posts — getAllPosts, getPostById, getPostsByUserId, createPost, updatePost, patchPost, deletePost
│   │   │   │   └── UsersEndpoint.java              # /users — getAllUsers, getUserById, getUserByUsername, deleteUser
│   │   │   └── models/
│   │   │       ├── Post.java                       # POJO for /posts resource (userId, id, title, body)
│   │   │       └── User.java                       # POJO for /users resource (with inner Address and Company classes)
│   │   │
│   │   ├── security/
│   │   │   ├── SecurityPayloads.java               # Centralised XSS, SQLi, and path-traversal payload constants
│   │   │   └── ZapManager.java                     # OWASP ZAP API client wrapper — session management, alert retrieval, HTML report export
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
│       │   │   ├── BaseTest.java                   # Parent for all UI test classes; manages driver lifecycle + ExtentReport init/flush
│       │   │   └── BaseApiTest.java                # Parent for all API test classes; ExtentReport lifecycle only — no WebDriver
│       │   │
│       │   └── tests/
│       │       ├── LoginTest.java                  # Login scenarios: valid, invalid, locked-out, empty credentials
│       │       ├── CheckoutFlowTest.java           # E2E checkout: login → add items → cart → checkout → confirm order
│       │       ├── PostsApiTest.java               # REST API tests for /posts (GET, POST, PUT, PATCH, DELETE)
│       │       ├── UsersApiTest.java               # REST API tests for /users (GET by id/username, DELETE, nested objects)
│       │       ├── SecurityHeadersTest.java        # HTTP response header assertions (HSTS, CSP, X-Frame-Options, etc.)
│       │       ├── InputValidationSecurityTest.java# DataProvider-driven XSS + SQLi payloads against login and checkout form fields
│       │       └── ZapPassiveScanTest.java         # Routes browser through OWASP ZAP proxy; asserts zero HIGH risk alerts
│       │
│       └── resources/
│           ├── config.properties                   # All runtime configuration (browser, timeouts, ZAP flags)
│           ├── testng.xml                          # TestNG suite definition
│           ├── dependency-check-suppressions.xml   # OWASP Dependency-Check false-positive suppression rules
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

**UI Layer:**
```
Tests  →  Page Objects  →  BasePage  →  WaitUtils  →  WebDriver
  ↓                                                        ↑
BaseTest  →  DriverFactory  →  DriverManager (ThreadLocal)
  ↓
TestListener  →  ExtentReportManager + VideoRecorder
```

**API Layer:**
```
API Tests  →  Endpoint Classes  →  BaseApiClient  →  REST Assured
    ↓                                    ↓
BaseApiTest  →  ExtentReportManager  (no WebDriver)
```

### Driver Management
`DriverFactory` creates a configured `WebDriver` for the requested browser. `DriverManager` stores it in a `ThreadLocal<WebDriver>`, ensuring every parallel thread gets its own isolated driver instance. Always use `DriverManager.getDriver()` — never store driver references in fields.

### Page Object Model
Every screen is a class in `com.automation.pages` that extends `BasePage`. Locators are declared as `@FindBy` fields (lazy PageFactory proxies). Interaction methods (`click`, `type`, `getText`) are inherited from `BasePage`, which adds built-in waits and stale-element retry automatically.

### Waits
All waits are centralised in `WaitUtils`. Use `forVisible()`, `forClickable()`, `fluent()`, etc. Never use raw `Thread.sleep` in test or page code — use `WaitUtils.hardWait()` only as a last resort.

### API Testing
`BaseApiClient` builds a shared `RequestSpecification` once (base URI, JSON content-type, `RequestLoggingFilter` + `ResponseLoggingFilter`). Endpoint classes extend it and expose typed methods returning the raw REST Assured `Response`. Test classes extend `BaseApiTest` — which handles only ExtentReport lifecycle (no WebDriver is created). The same `TestListener` covers both UI and API tests; all null-driver paths in `ScreenshotUtils` and `VideoRecorder` are already guarded, so no code changes were needed to the existing infrastructure.

### Security Layer
```
SecurityHeadersTest  →  REST Assured  →  base.url (HTTP GET)
InputValidationSecurityTest  →  Page Objects  →  WebDriver (XSS / SQLi DataProviders)
ZapPassiveScanTest  →  BaseTest (ZAP proxy)  →  ZapManager  →  ZAP REST API
OWASP Dependency-Check  →  pom.xml deps  →  NVD CVE database  (mvn verify)
```
`SecurityPayloads` centralises all attack strings. `ZapManager` wraps the ZAP Java client API — session reset, passive scan polling, alert retrieval, and HTML report export. `DriverFactory` routes browser traffic through ZAP automatically when `zap.enabled=true`.

### Reporting
`ExtentReportManager` is a double-checked locking singleton that writes `test-output/reports/ExtentReport_<timestamp>.html`. `TestListener` wires every TestNG event to both Log4j2 and ExtentReport, embedding a failure screenshot and the animated GIF recording for every UI test. API tests appear in the same report with full request/response logs.

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
| `api.base.url` | `https://jsonplaceholder.typicode.com` | | Base URL for REST Assured API tests |
| `zap.enabled` | `false` | `-Dzap.enabled=true` | Route browser through ZAP proxy for passive scanning |
| `zap.host` | `localhost` | `-Dzap.host=192.168.1.x` | Host where ZAP daemon is running |
| `zap.port` | `8080` | `-Dzap.port=8090` | Port ZAP daemon is listening on |

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

# Run tests by group — UI
mvn test -Dgroups=smoke      # smoke tests only: testSuccessfulLogin + testCompleteCheckoutFlow
mvn test -Dgroups=login      # all 4 LoginTest methods only
mvn test -Dgroups=checkout   # testCompleteCheckoutFlow only

# Run tests by group — API
mvn test -Dgroups=api        # all 13 API tests (Posts + Users)
mvn test -Dgroups=api,smoke  # smoke API tests only (5 tests)
mvn test -Dgroups=posts      # PostsApiTest only (7 tests)
mvn test -Dgroups=users      # UsersApiTest only (5 tests)

# Run tests by group — Security
mvn test -Dgroups=security         # all security tests (headers + injection)
mvn test -Dgroups=headers          # HTTP security header checks only (6 tests, no browser)
mvn test -Dgroups=injection        # XSS + SQLi input validation tests only (15 tests)
mvn test -Dgroups=zap -Dzap.enabled=true  # OWASP ZAP passive scan (ZAP must be running first)

# OWASP Dependency-Check CVE scan (runs on mvn verify, not mvn test)
mvn verify                         # full build + CVE scan — report at target/dependency-check-report.html
mvn dependency-check:check         # standalone CVE scan only

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

## API Test Coverage

Demo API: [JSONPlaceholder](https://jsonplaceholder.typicode.com) — a free, public REST API for testing.

### PostsApiTest (`/posts`)
| Test | Method | Assertion |
|---|---|---|
| `testGetAllPosts` | `GET /posts` | Status 200, 100 posts returned |
| `testGetPostById` | `GET /posts/1` | Status 200, correct id/userId/title/body |
| `testGetPostByIdNotFound` | `GET /posts/9999` | Status 404 |
| `testGetPostsByUserId` | `GET /posts?userId=1` | Status 200, 10 posts all with userId=1 |
| `testCreatePost` | `POST /posts` | Status 201, echoed body matches, id=101 |
| `testUpdatePost` | `PUT /posts/1` | Status 200, updated title/body reflected |
| `testPatchPost` | `PATCH /posts/1` | Status 200, only title changed |
| `testDeletePost` | `DELETE /posts/1` | Status 200, empty `{}` body |

### UsersApiTest (`/users`)
| Test | Method | Assertion |
|---|---|---|
| `testGetAllUsers` | `GET /users` | Status 200, exactly 10 users, nested address not null |
| `testGetUserById` | `GET /users/1` | Status 200, name/username correct, nested Address and Company deserialized |
| `testGetUserByIdNotFound` | `GET /users/9999` | Status 404 |
| `testGetUserByUsername` | `GET /users?username=Bret` | Status 200, exactly 1 match, correct id |
| `testDeleteUser` | `DELETE /users/1` | Status 200, empty `{}` body |

---

## Security Test Coverage

### SecurityHeadersTest — HTTP Response Header Checks
Fires a single REST Assured `GET` against `base.url` and asserts on the response headers. No browser required.

| Test | Header checked | Expected result |
|---|---|---|
| `testNoXPoweredByHeader` | `X-Powered-By` | Must be **absent** — exposes server technology |
| `testXFrameOptionsPresent` | `X-Frame-Options` | Must be `DENY` or `SAMEORIGIN` — prevents clickjacking |
| `testXContentTypeOptionsNosniff` | `X-Content-Type-Options` | Must be `nosniff` — prevents MIME-type sniffing |
| `testHstsPresent` | `Strict-Transport-Security` | Must be present with `max-age=` — enforces HTTPS |
| `testContentSecurityPolicyPresent` | `Content-Security-Policy` | Must be present — restricts script/frame origins |
| `testServerHeaderNoVersion` | `Server` | Must not include a version number |

> **Note:** saucedemo.com is a demo site and does not implement HSTS, X-Frame-Options, X-Content-Type-Options, or CSP. The 4 corresponding tests currently **fail by design** — they correctly identify real security gaps in the target site. The same tests would pass against a properly hardened production application.

### InputValidationSecurityTest — Injection & XSS
Selenium `@DataProvider` tests that submit malicious payloads through the UI and verify the application handles them safely.

| Test | Payloads | Assertion |
|---|---|---|
| `testXssInUsernameField` | 5 XSS strings | No JS alert fires; error message is shown |
| `testSqlInjectionInUsernameField` | 5 SQLi strings | URL does not reach `/inventory` (no auth bypass); error shown |
| `testXssInCheckoutFormFields` | 5 XSS strings | No JS alert fires after entering payload in first/last name fields |

All 15 injection tests **pass** against saucedemo.com.

### ZapPassiveScanTest — OWASP ZAP Passive Scan
Routes the complete Selenium session (login → products → cart → checkout) through OWASP ZAP acting as an intercepting proxy. ZAP inspects all request/response pairs without sending extra attack traffic.

| Test | Description |
|---|---|
| `browseAppThroughZap` | Navigates the full user journey so ZAP captures application traffic |
| `assertNoHighRiskAlerts` | Waits for passive scan to complete; asserts zero HIGH risk alerts; exports HTML report |

**Pre-requisites:**
```bash
# 1. Download OWASP ZAP — https://www.zaproxy.org/download/
# 2. Start ZAP as a daemon
zap.bat -daemon -port 8080 -host 0.0.0.0   # Windows
zap.sh  -daemon -port 8080 -host 0.0.0.0   # Linux / macOS
# 3. Run the scan
mvn test -Dgroups=zap -Dzap.enabled=true
```
ZAP tests **auto-skip** if ZAP is not reachable — `mvn test` stays green without any ZAP setup.
Report saved to `test-output/zap-passive-scan-report.html`.

### OWASP Dependency-Check — CVE Scan
Scans all Maven dependencies against the NVD (National Vulnerability Database). Fails the build for any dependency with CVSS ≥ 7. First run downloads the full NVD dataset (~200 MB, ~10 min). Subsequent runs use a local cache (~60 s).

```bash
mvn verify                    # CVE scan runs automatically as part of the verify phase
mvn dependency-check:check    # run the CVE scan standalone
```

Report: `target/dependency-check-report.html`. False positives can be suppressed in `src/test/resources/dependency-check-suppressions.xml`.

---

## Adding a New API Endpoint & Test

1. Create a POJO in `src/main/java/com/automation/api/models/` with `@JsonIgnoreProperties(ignoreUnknown = true)` and `@JsonProperty` per field.
2. Create an endpoint class in `src/main/java/com/automation/api/endpoints/` extending `BaseApiClient`. Each method calls `given().spec(BASE_SPEC)...` and returns `Response`.
3. Create a test class in `src/test/java/com/automation/tests/` extending `BaseApiTest`. Instantiate the endpoint inside each `@Test` method.
4. Register the test class in `src/test/resources/testng.xml`.

```java
public class CommentsEndpoint extends BaseApiClient {
    public Response getCommentsByPostId(int postId) {
        return given().spec(BASE_SPEC)
                .queryParam("postId", postId)
                .when().get("/comments");
    }
}

public class CommentsApiTest extends BaseApiTest {
    @Test(groups = {"api", "comments"})
    public void testGetCommentsByPostId() {
        Response response = new CommentsEndpoint().getCommentsByPostId(1);
        response.then().statusCode(200);
        Assert.assertEquals(response.jsonPath().getList("").size(), 5);
    }
}
```

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
| ZAP Scan Report | `test-output/zap-passive-scan-report.html` | OWASP ZAP passive scan findings (generated when `zap.enabled=true`) |
| CVE Report | `target/dependency-check-report.html` | Dependency CVE scan results (generated by `mvn verify`) |

---

## CI/CD

For pipelines with no display server (e.g. GitHub Actions, Jenkins):

```bash
mvn clean test -Dheadless=true -Dvideo.enabled=false
```

`VideoRecorder` automatically disables itself when `headless=true` or when `GraphicsEnvironment.isHeadless()` returns true — no configuration change needed in CI environments.
