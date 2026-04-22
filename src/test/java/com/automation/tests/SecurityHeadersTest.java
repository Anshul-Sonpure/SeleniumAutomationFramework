package com.automation.tests;

import com.automation.base.BaseApiTest;
import com.automation.utils.ConfigReader;
import com.automation.utils.ExtentReportManager;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static io.restassured.RestAssured.given;

/**
 * SecurityHeadersTest — verifies that the application's HTTP responses include
 * the security headers recommended by OWASP and the WHATWG Fetch spec.
 *
 * WHY THESE HEADERS MATTER:
 *   X-Frame-Options          — prevents clickjacking attacks (UI redress)
 *   X-Content-Type-Options   — stops browsers from MIME-sniffing responses
 *   Strict-Transport-Security— forces HTTPS for all future requests (HSTS)
 *   Content-Security-Policy  — restricts origins for scripts/styles/frames
 *   X-Powered-By absent      — hides server/framework identity from attackers
 *   Server (no version)      — prevents banner grabbing / targeted exploits
 *
 * All tests in this class are in the "security" and "headers" groups so they
 * can be run independently: mvn test -Dgroups=headers
 */
public class SecurityHeadersTest extends BaseApiTest {

    private static final Logger log = LogManager.getLogger(SecurityHeadersTest.class);

    // One HTTP GET is made per class and the response is shared across all tests.
    private static Response pageResponse;

    @BeforeClass(alwaysRun = true)
    public void fetchPage() {
        String targetUrl = ConfigReader.get("base.url", "https://www.saucedemo.com");
        pageResponse = given()
                .relaxedHTTPSValidation()
                .when().get(targetUrl)
                .then().extract().response();
        log.info("Security headers check — target: {} | status: {}", targetUrl, pageResponse.statusCode());
    }

    @Test(description = "X-Powered-By header should be absent — its presence leaks the server technology stack",
          groups = {"security", "headers"})
    public void testNoXPoweredByHeader() {
        String header = pageResponse.header("X-Powered-By");
        Assert.assertNull(header,
                "X-Powered-By header should NOT be present — it exposes server technology: " + header);
        ExtentReportManager.getTest().pass("X-Powered-By header is absent");
    }

    @Test(description = "X-Frame-Options should be DENY or SAMEORIGIN to prevent clickjacking",
          groups = {"security", "headers"})
    public void testXFrameOptionsPresent() {
        SoftAssert sa = new SoftAssert();
        String header = pageResponse.header("X-Frame-Options");
        sa.assertNotNull(header, "X-Frame-Options header is missing — app may be vulnerable to clickjacking");
        if (header != null) {
            boolean valid = header.equalsIgnoreCase("DENY") || header.equalsIgnoreCase("SAMEORIGIN");
            sa.assertTrue(valid,
                    "X-Frame-Options value '" + header + "' must be DENY or SAMEORIGIN");
            ExtentReportManager.getTest().info("X-Frame-Options: " + header);
        }
        sa.assertAll();
    }

    @Test(description = "X-Content-Type-Options must be 'nosniff' to prevent MIME-type sniffing attacks",
          groups = {"security", "headers"})
    public void testXContentTypeOptionsNosniff() {
        String header = pageResponse.header("X-Content-Type-Options");
        Assert.assertNotNull(header, "X-Content-Type-Options header is missing");
        Assert.assertEquals(header.toLowerCase(), "nosniff",
                "X-Content-Type-Options must be 'nosniff', got: " + header);
        ExtentReportManager.getTest().pass("X-Content-Type-Options: " + header);
    }

    @Test(description = "Strict-Transport-Security (HSTS) header must be present with a max-age directive",
          groups = {"security", "headers"})
    public void testHstsPresent() {
        String header = pageResponse.header("Strict-Transport-Security");
        Assert.assertNotNull(header,
                "Strict-Transport-Security (HSTS) header is missing — HTTPS is not enforced via headers");
        Assert.assertTrue(header.contains("max-age="),
                "HSTS header must include a max-age directive, got: " + header);
        ExtentReportManager.getTest().pass("HSTS: " + header);
    }

    @Test(description = "Content-Security-Policy header should be present to limit XSS impact",
          groups = {"security", "headers"})
    public void testContentSecurityPolicyPresent() {
        SoftAssert sa = new SoftAssert();
        String header = pageResponse.header("Content-Security-Policy");
        sa.assertNotNull(header,
                "Content-Security-Policy (CSP) header is missing — no policy restricts inline script execution");
        if (header != null) {
            ExtentReportManager.getTest().info("CSP: " + header);
        }
        sa.assertAll();
    }

    @Test(description = "Server header should not include a version number to prevent targeted exploits",
          groups = {"security", "headers"})
    public void testServerHeaderNoVersion() {
        String serverHeader = pageResponse.header("Server");
        if (serverHeader != null) {
            // "nginx/1.18.0" or "Apache/2.4.41" both leak exploitable version info.
            boolean leaksVersion = serverHeader.matches(".*\\/[0-9].*");
            Assert.assertFalse(leaksVersion,
                    "Server header leaks version info: '" + serverHeader
                    + "' — strip the version from server configuration");
            ExtentReportManager.getTest().info("Server header: " + serverHeader + " (no version leaked)");
        } else {
            ExtentReportManager.getTest().info("Server header absent — ideal configuration");
        }
    }
}
