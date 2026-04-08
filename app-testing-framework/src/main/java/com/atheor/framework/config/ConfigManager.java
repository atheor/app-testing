package com.atheor.framework.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads test configuration from {@code test.properties} on the classpath.
 * Any property can be overridden at runtime via a JVM system property
 * (e.g. {@code -Dbrowser=edge}).
 */
public final class ConfigManager {

    private static final String DEFAULT_PROPERTIES_FILE = "test.properties";
    private static final Properties properties = new Properties();

    static {
        try (InputStream is = ConfigManager.class.getClassLoader()
                .getResourceAsStream(DEFAULT_PROPERTIES_FILE)) {
            if (is != null) {
                properties.load(is);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(
                    "Failed to load " + DEFAULT_PROPERTIES_FILE + ": " + e.getMessage());
        }
    }

    private ConfigManager() {}

    /**
     * Returns the property value, with system property taking precedence over
     * the properties file.
     */
    public static String get(String key) {
        return System.getProperty(key, properties.getProperty(key));
    }

    /**
     * Returns the property value or the supplied default when the key is absent.
     */
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return (value != null) ? value : defaultValue;
    }

    /**
     * Returns the property as an {@code int}.
     */
    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the property as a {@code boolean}.
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return (value == null) ? defaultValue : Boolean.parseBoolean(value.trim());
    }
}
