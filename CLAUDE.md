# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
mvn clean test                    # Full clean + run all tests
mvn test                          # Run all tests (using testng.xml suite)
mvn test -Dbrowser=firefox        # Override browser at runtime
mvn test -Dheadless=true          # Run headless (for CI)
mvn test -Dgroups=smoke           # Run only smoke tests

# Security testing
mvn test -Dgroups=security        # All security tests (headers + injection)
mvn test -Dgroups=headers         # HTTP security header checks only
mvn test -Dgroups=injection       # XSS + SQLi input validation tests only
mvn test -Dgroups=zap -Dzap.enabled=true   # ZAP passive scan (ZAP must be running)
mvn verify                        # Runs OWASP Dependency-Check (CVE scan of dependencies)
mvn dependency-check:check        # Standalone dependency CVE scan
```

TestNG suite is at `src/test/resources/testng.xml`. Parallel mode is `methods` with thread-count 3. To run a single test class, add it directly to `testng.xml` or use a custom suite file.

## Architecture

This is a Java 11 + Selenium 4 + TestNG UI test automation framework targeting `https://www.saucedemo.com`. It is structured around the Page Object Model (POM).

### Layers

**Driver management** — `DriverFactory` creates a browser-specific `WebDriver` (Chrome/Firefox/Edge) using WebDriverManager. `DriverManager` wraps it in a `ThreadLocal<WebDriver>`, making all driver access thread-safe for parallel execution. Always use `DriverManager.getDriver()` / `removeDriver()`, never store driver instances in fields.

**Base classes** — `BasePage` (main) is the parent of all Page Objects. Its constructor calls `PageFactory.initElements`, and it exposes wrapped element interactions that delegate to `WaitUtils`. `BaseTest` (test) is the parent of all test classes. It manages the full lifecycle: ExtentReport init (`@BeforeSuite`), driver creation + navigation (`@BeforeMethod`), and driver quit + ThreadLocal cleanup (`@AfterMethod`/`@AfterSuite`).

**Configuration** — `ConfigReader` loads `src/test/resources/config.properties` once at class-load. System properties (`-Dbrowser=`, `-Dheadless=`) always win over file values.

**Waits** — All waits are centralised in `WaitUtils`. Use `WaitUtils.forVisible()`, `forClickable()`, `fluent()`, etc. Do not use raw `Thread.sleep` except via `WaitUtils.hardWait()`. Timeouts (`explicit.wait`, `polling.interval`) come from config.

**Reporting** — `ExtentReportManager` is a double-checked locking singleton writing HTML to `test-output/ExtentReport.html`. It uses a `ThreadLocal<ExtentTest>` for parallel-safe per-test logging. `ScreenshotUtils` saves PNGs to `test-output/screenshots/` and can return Base64 bytes for inline embedding. `TestListener` wires TestNG events to both Log4j2 and ExtentReport, embedding a screenshot on failure.

### Adding a new Page Object

1. Create a class in `src/main/java/com/automation/pages/` extending `BasePage`.
2. Declare `@FindBy` fields for locators.
3. Use the inherited `click()`, `type()`, `getText()`, etc. methods — these include built-in waits.

### Adding a new test class

1. Create a class in `src/test/java/com/automation/tests/` extending `BaseTest`.
2. Use Page Object instances; avoid raw `By` locators directly in tests (see `SampleLoginTest` as a counter-example to avoid).
3. Register the class in `src/test/resources/testng.xml`.

## Key Config

| Property | Default | Notes |
|---|---|---|
| `browser` | `chrome` | Overridable with `-Dbrowser=` |
| `headless` | `false` | Overridable with `-Dheadless=` |
| `base.url` | `https://www.saucedemo.com` | Navigation target in `@BeforeMethod` |
| `explicit.wait` | `10` (seconds) | Used by `WaitUtils` |
| `page.load.timeout` | `30` (seconds) | Set on driver in `BaseTest` |
| `zap.enabled` | `false` | Routes browser through ZAP proxy when `true` |
| `zap.host` | `localhost` | ZAP daemon host |
| `zap.port` | `8080` | ZAP daemon port |

Logs roll daily or at 10 MB to `logs/automation.log` (10 rotations kept). Selenium/WebDriverManager loggers are suppressed to `WARN`.

## Security Testing

Four security layers are integrated:

### 1. OWASP Dependency-Check (`mvn verify`)
Scans all Maven dependencies against the NVD CVE database. Fails the build on CVSS ≥ 7. Report: `target/dependency-check-report.html`. False positives are suppressed via `src/test/resources/dependency-check-suppressions.xml`.

### 2. Security Header Checks (`SecurityHeadersTest`)
REST Assured assertions that the application's HTTP responses include recommended headers: `X-Frame-Options`, `X-Content-Type-Options`, `Strict-Transport-Security`, `Content-Security-Policy`. Also asserts that `X-Powered-By` is absent and `Server` does not leak version numbers.

### 3. Input Validation / Injection Tests (`InputValidationSecurityTest`)
Selenium @DataProvider tests that submit XSS and SQL injection payloads through the login form and checkout form. Asserts: no JS alert fires (reflected XSS), SQLi does not bypass authentication, proper error messages appear. Payloads centralised in `SecurityPayloads.java`.

### 4. OWASP ZAP Passive Scan (`ZapPassiveScanTest`)
Routes the full Selenium browser session through OWASP ZAP acting as a proxy. After browsing login → products → cart → checkout, queries the ZAP API for passive scan findings and asserts zero HIGH risk alerts. Exports `test-output/zap-passive-scan-report.html`.

**ZAP pre-requisites:**
```bash
# Download ZAP from https://www.zaproxy.org/download/
zap.sh -daemon -port 8080 -host 0.0.0.0   # Linux/macOS
zap.bat -daemon -port 8080 -host 0.0.0.0  # Windows
# Then run tests:
mvn test -Dgroups=zap -Dzap.enabled=true
```
Tests auto-skip if ZAP is not reachable — safe to leave in the default suite.
