package com.automation.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * ConfigReader — Single point of access for all test configuration values.
 *
 * HOW IT WORKS:
 * ──────────────
 * Configuration is read from src/test/resources/config.properties.
 * The file is placed on the classpath by Maven, so it can be loaded as a
 * resource stream (no hardcoded file path needed — works on any OS).
 *
 * SYSTEM PROPERTY OVERRIDE:
 * ──────────────────────────
 * Every getter checks System.getProperty(key) FIRST, before the file value.
 * This lets CI/CD pipelines or command-line runs override settings without
 * editing the file:
 *
 *   mvn test -Dbrowser=firefox -Dheadless=true
 *
 * This is the standard Maven/TestNG pattern for parameterised test execution.
 *
 * STATIC INITIALIZER:
 * ────────────────────
 * The Properties object is loaded once when the class is first referenced
 * (i.e. when the JVM loads the class). This avoids re-reading the file on
 * every get() call — important for performance in large suites.
 */
public class ConfigReader {

    private static final Logger log = LogManager.getLogger(ConfigReader.class);

    // Stores all key=value pairs loaded from config.properties.
    private static final Properties props = new Properties();

    /**
     * Static initializer block — runs exactly once when ConfigReader is first loaded.
     * try-with-resources ensures the InputStream is closed automatically,
     * even if an exception is thrown during props.load().
     */
    static {
        try (InputStream in = ConfigReader.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {

            if (in == null) {
                // File not found on the classpath — warn but don't crash.
                // Tests will still run using System property overrides or defaults.
                log.warn("config.properties not found on classpath — using defaults/system properties");
            } else {
                props.load(in);
                log.info("config.properties loaded successfully");
            }

        } catch (IOException e) {
            log.error("Error loading config.properties: {}", e.getMessage());
        }
    }

    // Private constructor — prevents instantiation (utility class pattern).
    private ConfigReader() {}

    /**
     * Returns the value for 'key', checking System properties first.
     * Returns null if the key is not found anywhere.
     *
     * @param key  property key (e.g. "browser")
     */
    public static String get(String key) {
        return System.getProperty(key, props.getProperty(key));
    }

    /**
     * Returns the value for 'key', falling back to 'defaultValue' if not found.
     * This is the preferred overload — always provide a sensible default.
     *
     * @param key           property key
     * @param defaultValue  value to return when the key is absent
     */
    public static String get(String key, String defaultValue) {
        return System.getProperty(key, props.getProperty(key, defaultValue));
    }

    /**
     * Returns an integer property value.
     * Useful for timeout and thread-count configuration.
     *
     * @param key           property key (e.g. "explicit.wait")
     * @param defaultValue  fallback integer if the key is missing or not a number
     */
    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            // Log the bad value and fall through to the default.
            log.warn("Property '{}' has non-integer value; using default {}", key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Returns a boolean property value.
     * Boolean.parseBoolean is lenient: "true" (any case) → true; anything else → false.
     *
     * @param key           property key (e.g. "headless")
     * @param defaultValue  fallback boolean
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }
}
