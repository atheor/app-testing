package com.atheor.framework.api;

import com.atheor.framework.config.ConfigManager;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;

/**
 * Factory for pre-configured REST Assured {@link RequestSpecification} instances.
 *
 * <p>Base URI and logging behaviour are driven by {@link ConfigManager} properties:
 * <ul>
 *   <li>{@code rest.base.uri} — base URI (default: empty)</li>
 *   <li>{@code rest.logging.enabled} — {@code true} to log all requests/responses (default: false)</li>
 * </ul>
 * </p>
 */
public final class RestAssuredClient {

    private RestAssuredClient() {}

    /**
     * Returns a {@link RequestSpecification} configured with the base URI from
     * {@code test.properties} and optional request/response logging.
     */
    public static RequestSpecification buildSpec() {
        return buildSpec(ConfigManager.get("rest.base.uri", ""));
    }

    /**
     * Returns a {@link RequestSpecification} configured with the supplied base URI
     * and optional request/response logging.
     *
     * @param baseUri the base URI for all requests made with this spec
     */
    public static RequestSpecification buildSpec(String baseUri) {
        RequestSpecBuilder builder = new RequestSpecBuilder();

        if (baseUri != null && !baseUri.isBlank()) {
            builder.setBaseUri(baseUri);
        }

        boolean loggingEnabled = ConfigManager.getBoolean("rest.logging.enabled", false);
        if (loggingEnabled) {
            builder.addFilter(new RequestLoggingFilter());
            builder.addFilter(new ResponseLoggingFilter());
        }

        return RestAssured.given().spec(builder.build());
    }
}
