package com.automation.api;

import com.automation.utils.ConfigReader;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Configures the shared RequestSpecification used by all endpoint classes.
// Built once in a static initializer; endpoint subclasses reference BASE_SPEC directly.
public class BaseApiClient {

    private static final Logger log = LogManager.getLogger(BaseApiClient.class);

    protected static final RequestSpecification BASE_SPEC;

    static {
        String baseUri = ConfigReader.get("api.base.url", "https://jsonplaceholder.typicode.com");
        log.info("Initialising REST Assured with base URI: {}", baseUri);

        RestAssured.baseURI = baseUri;

        BASE_SPEC = new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .build();
    }

    protected BaseApiClient() {}
}
