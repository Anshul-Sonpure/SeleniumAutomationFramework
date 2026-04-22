package com.automation.security;

// Attack payload constants shared across all injection-focused security tests.
public final class SecurityPayloads {

    // XSS payloads covering reflected, DOM-based, and attribute injection vectors.
    public static final String[] XSS_PAYLOADS = {
        "<script>alert(1)</script>",
        "<img src=x onerror=alert(1)>",
        "'\"><script>alert(1)</script>",
        "<svg/onload=alert(1)>",
        "javascript:alert(1)"
    };

    // SQL injection payloads targeting classic auth-bypass and destructive patterns.
    public static final String[] SQLI_PAYLOADS = {
        "' OR '1'='1",
        "' OR '1'='1'--",
        "admin'--",
        "' OR 1=1--",
        "'; DROP TABLE users;--"
    };

    // Path traversal payloads for any endpoint that accepts file or path parameters.
    public static final String[] PATH_TRAVERSAL_PAYLOADS = {
        "../../../etc/passwd",
        "..%2F..%2F..%2Fetc%2Fpasswd",
        "....//....//etc/passwd"
    };

    private SecurityPayloads() {}
}
