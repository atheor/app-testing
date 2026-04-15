package com.atheor.framework.payload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads XML payload templates from classpath resources and replaces ${token} placeholders.
 */
public final class PayloadFileLoader {

    private final String template;
    private final Map<String, String> tokens = new LinkedHashMap<>();

    private PayloadFileLoader(String template) {
        this.template = template;
    }

    public static PayloadFileLoader fromClasspath(String classpathResource) throws IOException {
        try (InputStream is = PayloadFileLoader.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (is == null) {
                throw new IOException("Classpath resource not found: " + classpathResource);
            }
            return new PayloadFileLoader(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    public PayloadFileLoader with(String token, String value) {
        tokens.put(token, escapeXml(value));
        return this;
    }

    public PayloadFileLoader withRaw(String token, String value) {
        tokens.put(token, value == null ? "" : value);
        return this;
    }

    public String build() {
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