package com.atheor.e2e.adms.payload;

import java.util.Objects;

/**
 * Option 2 — Builder-based SOAP payload for the {@code CreateOperation} operation.
 *
 * <p>Constructs a complete WS-Security–signed SOAP 1.1 envelope from typed fields.
 * All values are XML-escaped automatically.
 *
 * <p>Usage:
 * <pre>{@code
 * String envelope = CreateOperationPayload.builder()
 *     .username("adms_user")
 *     .password("adms_password")
 *     .scheduleId("SCH-001")
 *     .operationName("Test Operation")
 *     .deviceId("DEVICE-001")
 *     .build()
 *     .toEnvelope();
 * }</pre>
 */
public class CreateOperationPayload implements SoapPayloadSource {

    private static final String NAMESPACE = "urn:wpm";

    private final String username;
    private final String password;
    private final String scheduleId;
    private final String operationName;
    private final String deviceId;

    private CreateOperationPayload(Builder builder) {
        this.username      = builder.username;
        this.password      = builder.password;
        this.scheduleId    = builder.scheduleId;
        this.operationName = builder.operationName;
        this.deviceId      = builder.deviceId;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toEnvelope() {
        return """
                <?xml version="1.0" encoding="utf-8"?>
                <soapenv:Envelope xmlns:soapenv="%s"
                                  xmlns:tns="%s">
                    <soapenv:Header>
                        <wsse:Security xmlns:wsse="%s">
                            <wsse:UsernameToken>
                                <wsse:Username>%s</wsse:Username>
                                <wsse:Password Type="%s">%s</wsse:Password>
                            </wsse:UsernameToken>
                        </wsse:Security>
                    </soapenv:Header>
                    <soapenv:Body>
                        <tns:CreateOperation>
                            <tns:CreateOperationTc>
                                <tns:scheduleId>%s</tns:scheduleId>
                                <tns:operationName>%s</tns:operationName>
                                <tns:deviceId>%s</tns:deviceId>
                            </tns:CreateOperationTc>
                        </tns:CreateOperation>
                    </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(
                        SOAPENV_NS, NAMESPACE,
                        WSSE_NS, esc(username), PASSWORD_TEXT_TYPE, esc(password),
                        esc(scheduleId), esc(operationName), esc(deviceId));
    }

    private static String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }

    // ---- Builder ----

    public static class Builder {

        private String username;
        private String password;
        private String scheduleId;
        private String operationName;
        private String deviceId;

        private Builder() {}

        public Builder username(String username)           { this.username = username;           return this; }
        public Builder password(String password)           { this.password = password;           return this; }
        public Builder scheduleId(String scheduleId)       { this.scheduleId = scheduleId;       return this; }
        public Builder operationName(String operationName) { this.operationName = operationName; return this; }
        public Builder deviceId(String deviceId)           { this.deviceId = deviceId;           return this; }

        public CreateOperationPayload build() {
            Objects.requireNonNull(username,      "username is required");
            Objects.requireNonNull(password,      "password is required");
            Objects.requireNonNull(scheduleId,    "scheduleId is required");
            Objects.requireNonNull(operationName, "operationName is required");
            Objects.requireNonNull(deviceId,      "deviceId is required");
            return new CreateOperationPayload(this);
        }
    }
}
