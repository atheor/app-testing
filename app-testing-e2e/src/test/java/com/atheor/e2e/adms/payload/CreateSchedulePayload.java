package com.atheor.e2e.adms.payload;

import java.util.Objects;

/**
 * Option 2 — Builder-based SOAP payload for the {@code CreateSchedule} operation.
 *
 * <p>Constructs a complete WS-Security–signed SOAP 1.1 envelope from typed fields.
 * All values are XML-escaped automatically.
 *
 * <p>Usage:
 * <pre>{@code
 * String envelope = CreateSchedulePayload.builder()
 *     .username("adms_user")
 *     .password("adms_password")
 *     .scheduleName("Test Schedule")
 *     .startDate("2026-04-15T08:00:00")
 *     .endDate("2026-04-15T17:00:00")
 *     .build()
 *     .toEnvelope();
 * }</pre>
 */
public class CreateSchedulePayload implements SoapPayloadSource {

    private static final String NAMESPACE = "urn:wpm";

    private final String username;
    private final String password;
    private final String scheduleName;
    private final String startDate;
    private final String endDate;

    private CreateSchedulePayload(Builder builder) {
        this.username     = builder.username;
        this.password     = builder.password;
        this.scheduleName = builder.scheduleName;
        this.startDate    = builder.startDate;
        this.endDate      = builder.endDate;
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
                        <tns:CreateSchedule>
                            <tns:CreateScheduleTc>
                                <tns:scheduleName>%s</tns:scheduleName>
                                <tns:startDate>%s</tns:startDate>
                                <tns:endDate>%s</tns:endDate>
                            </tns:CreateScheduleTc>
                        </tns:CreateSchedule>
                    </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(
                        SOAPENV_NS, NAMESPACE,
                        WSSE_NS, esc(username), PASSWORD_TEXT_TYPE, esc(password),
                        esc(scheduleName), esc(startDate), esc(endDate));
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
        private String scheduleName;
        private String startDate;
        private String endDate;

        private Builder() {}

        public Builder username(String username)         { this.username = username;         return this; }
        public Builder password(String password)         { this.password = password;         return this; }
        public Builder scheduleName(String scheduleName) { this.scheduleName = scheduleName; return this; }
        public Builder startDate(String startDate)       { this.startDate = startDate;       return this; }
        public Builder endDate(String endDate)           { this.endDate = endDate;           return this; }

        public CreateSchedulePayload build() {
            Objects.requireNonNull(username,     "username is required");
            Objects.requireNonNull(password,     "password is required");
            Objects.requireNonNull(scheduleName, "scheduleName is required");
            Objects.requireNonNull(startDate,    "startDate is required");
            Objects.requireNonNull(endDate,      "endDate is required");
            return new CreateSchedulePayload(this);
        }
    }
}
