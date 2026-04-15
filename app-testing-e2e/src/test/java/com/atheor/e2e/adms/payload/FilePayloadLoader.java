package com.atheor.e2e.adms.payload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Option 1 — File-based SOAP payload.
 *
 * <p>Loads a complete SOAP envelope template from a classpath XML file and substitutes
 * {@code ${token}} placeholders with values supplied via {@link #with(String, String)}.
 * Values are XML-escaped automatically to prevent injection.
 *
 * <p>Usage:
 * <pre>{@code
 * String envelope = FilePayloadLoader
 *     .from("payloads/adms/create_schedule.xml")
 *     .with("username",     "adms_user")
 *     .with("password",     "adms_password")
 *     .with("scheduleName", "Test Schedule")
 *     .with("startDate",    "2026-04-15T08:00:00")
 *     .with("endDate",      "2026-04-15T17:00:00")
 *     .toEnvelope();
 * }</pre>
 *
 * <p>Use {@link #withRaw(String, String)} when the value is already valid XML and must
 * not be escaped (e.g. when injecting a pre-built XML fragment).
 */
public class FilePayloadLoader implements SoapPayloadSource {

    private final String template;
    private final Map<String, String> tokens = new LinkedHashMap<>();

    private FilePayloadLoader(String template) {
        this.template = template;
    }

    /**
     * Loads the SOAP envelope template from a classpath resource.
     *
     * @param classpathResource resource path relative to the classpath root,
     *                          e.g. {@code "payloads/adms/create_schedule.xml"}
     * @return a new {@code FilePayloadLoader} ready for token substitution
     * @throws IOException if the resource cannot be found or read
     */
    public static FilePayloadLoader from(String classpathResource) throws IOException {
        try (InputStream is = FilePayloadLoader.class.getClassLoader()
                .getResourceAsStream(classpathResource)) {
            if (is == null) {
                throw new IOException("Classpath resource not found: " + classpathResource);
            }
            String template = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return new FilePayloadLoader(template);
        }
    }

    /**
     * Registers a token replacement. The {@code value} is XML-escaped before substitution.
     *
     * @param token placeholder name as it appears in the template without the {@code ${ }} delimiters
     * @param value the replacement value; {@code null} is treated as an empty string
     * @return this loader (fluent)
     */
    public FilePayloadLoader with(String token, String value) {
        tokens.put(token, escapeXml(value));
        return this;
    }

    /**
     * Registers a token replacement without XML escaping.
     * Use this only when the value is already valid, trusted XML.
     *
     * @param token placeholder name
     * @param value raw replacement value; {@code null} is treated as an empty string
     * @return this loader (fluent)
     */
    public FilePayloadLoader withRaw(String token, String value) {
        tokens.put(token, value == null ? "" : value);
        return this;
    }

    /**
     * Returns the template with all registered tokens substituted.
     * Any {@code ${token}} that was not registered is left unchanged in the output.
     */
    @Override
    public String toEnvelope() {
        String result = template;
        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private static String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }
}
