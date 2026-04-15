package com.atheor.e2e.adms.client;

import com.atheor.e2e.adms.payload.SoapPayloadSource;
import com.atheor.framework.config.ConfigManager;
import com.atheor.framework.soap.SoapClient;

import java.io.IOException;

/**
 * Typed SOAP client for the GE ADMS inbound service.
 *
 * <p>All three operations share the same endpoint and use WS-Security
 * UsernameToken (PasswordText) in the SOAP header.
 *
 * <p>Namespace and element names align with {@code inbound.wsdl} (targetNamespace {@code wpm}).
 * Operations defined in the WSDL: {@code CreateSchedule}, {@code CreateOperation}.
 * {@code PrecheckPlannedOrder} follows the same conventions (WSDL pending).
 */
public class AdmsSoapClient {

    private static final String DEFAULT_ENDPOINT = "http://your-adms-host/AdmsService";

    private static final String NAMESPACE = "urn:wpm";

    private static final String WSSE_NS =
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private static final String PASSWORD_TEXT_TYPE =
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText";

    private final String endpoint;
    private final String username;
    private final String password;

    public AdmsSoapClient() {
        this.endpoint  = ConfigManager.get("adms.endpoint",  DEFAULT_ENDPOINT);
        this.username  = ConfigManager.get("adms.username",  "adms_user");
        this.password  = ConfigManager.get("adms.password",  "adms_password");
    }

    // ---- Public operations ----

    /**
     * Calls the {@code CreateSchedule} operation.
     *
     * @param scheduleName human-readable name for the schedule
     * @param startDate    ISO-8601 date string, e.g. {@code "2026-04-14T08:00:00"}
     * @param endDate      ISO-8601 date string, e.g. {@code "2026-04-14T17:00:00"}
     * @return raw SOAP response body
     */
    public String createSchedule(String scheduleName, String startDate, String endDate) throws IOException {
        String body = """
                <tns:CreateScheduleTc>
                            <tns:scheduleName>%s</tns:scheduleName>
                            <tns:startDate>%s</tns:startDate>
                            <tns:endDate>%s</tns:endDate>
                        </tns:CreateScheduleTc>""".formatted(
                escapeXml(scheduleName), escapeXml(startDate), escapeXml(endDate));

        String envelope = buildEnvelope("CreateSchedule", body);
        return new SoapClient(NAMESPACE + "/CreateSchedule").sendRequest(endpoint, envelope);
    }

    /**
     * Calls the {@code CreateOperation} operation.
     *
     * @param scheduleId    ID returned by a prior {@code CreateSchedule} call
     * @param operationName human-readable name for the switching operation
     * @param deviceId      identifier of the network device involved
     * @return raw SOAP response body
     */
    public String createOperation(String scheduleId, String operationName, String deviceId) throws IOException {
        String body = """
                <tns:CreateOperationTc>
                            <tns:scheduleId>%s</tns:scheduleId>
                            <tns:operationName>%s</tns:operationName>
                            <tns:deviceId>%s</tns:deviceId>
                        </tns:CreateOperationTc>""".formatted(
                escapeXml(scheduleId), escapeXml(operationName), escapeXml(deviceId));

        String envelope = buildEnvelope("CreateOperation", body);
        return new SoapClient(NAMESPACE + "/CreateOperation").sendRequest(endpoint, envelope);
    }

    /**
     * Calls the {@code PrecheckPlannedOrder} operation.
     *
     * @param operationId ID returned by a prior {@code CreateOperation} call
     * @param deviceId    identifier of the network device to validate against
     * @return raw SOAP response body
     */
    public String precheckPlannedOrder(String operationId, String deviceId) throws IOException {
        String body = """
                <tns:operationId>%s</tns:operationId>
                        <tns:deviceId>%s</tns:deviceId>""".formatted(
                escapeXml(operationId), escapeXml(deviceId));

        String envelope = buildEnvelope("PrecheckPlannedOrder", body);
        return new SoapClient(NAMESPACE + "/PrecheckPlannedOrder").sendRequest(endpoint, envelope);
    }

    /**
     * Sends a fully assembled {@code CreateSchedule} SOAP envelope.
     */
    public String createScheduleFromEnvelope(String envelope) throws IOException {
        return new SoapClient(NAMESPACE + "/CreateSchedule").sendRequest(endpoint, envelope);
    }

    /**
     * Sends a fully assembled {@code CreateOperation} SOAP envelope.
     */
    public String createOperationFromEnvelope(String envelope) throws IOException {
        return new SoapClient(NAMESPACE + "/CreateOperation").sendRequest(endpoint, envelope);
    }

    /**
     * Sends a fully assembled {@code PrecheckPlannedOrder} SOAP envelope.
     */
    public String precheckPlannedOrderFromEnvelope(String envelope) throws IOException {
        return new SoapClient(NAMESPACE + "/PrecheckPlannedOrder").sendRequest(endpoint, envelope);
    }

    // ---- SoapPayloadSource overloads ----

    /**
     * Sends a {@code CreateSchedule} request using a pre-built {@link SoapPayloadSource}.
      * Use {@link com.atheor.framework.payload.PayloadFileLoader} or
     * {@link com.atheor.e2e.adms.payload.CreateSchedulePayload} to create the payload.
     *
     * @param payload the fully assembled SOAP envelope source
     * @return raw SOAP response body
     */
    public String createSchedule(SoapPayloadSource payload) throws IOException {
        return new SoapClient(NAMESPACE + "/CreateSchedule").sendRequest(endpoint, payload.toEnvelope());
    }

    /**
     * Sends a {@code CreateOperation} request using a pre-built {@link SoapPayloadSource}.
      * Use {@link com.atheor.framework.payload.PayloadFileLoader} or
     * {@link com.atheor.e2e.adms.payload.CreateOperationPayload} to create the payload.
     *
     * @param payload the fully assembled SOAP envelope source
     * @return raw SOAP response body
     */
    public String createOperation(SoapPayloadSource payload) throws IOException {
        return new SoapClient(NAMESPACE + "/CreateOperation").sendRequest(endpoint, payload.toEnvelope());
    }

    /**
     * Sends a {@code PrecheckPlannedOrder} request using a pre-built {@link SoapPayloadSource}.
     *
     * @param payload the fully assembled SOAP envelope source
     * @return raw SOAP response body
     */
    public String precheckPlannedOrder(SoapPayloadSource payload) throws IOException {
        return new SoapClient(NAMESPACE + "/PrecheckPlannedOrder").sendRequest(endpoint, payload.toEnvelope());
    }

    // ---- Helpers ----

    private String buildEnvelope(String operationName, String body) {
        return """
                <?xml version="1.0" encoding="utf-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:tns="%s">
                    %s
                    <soapenv:Body>
                        <tns:%s>
                            %s
                        </tns:%s>
                    </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(NAMESPACE, buildSecurityHeader(), operationName, body, operationName);
    }

    private String buildSecurityHeader() {
        return """
                <soapenv:Header>
                        <wsse:Security xmlns:wsse="%s">
                            <wsse:UsernameToken>
                                <wsse:Username>%s</wsse:Username>
                                <wsse:Password Type="%s">%s</wsse:Password>
                            </wsse:UsernameToken>
                        </wsse:Security>
                    </soapenv:Header>""".formatted(WSSE_NS, escapeXml(username), PASSWORD_TEXT_TYPE, escapeXml(password));
    }

    /**
     * Extracts the text content of the first occurrence of {@code <tagName>...</tagName>}
     * in the given XML string.
     *
     * @param xml     the XML string to search
     * @param tagName the local element name (without namespace prefix)
     * @return the element text, or {@code null} if not found
     */
    public String extractElement(String xml, String tagName) {
        if (xml == null || tagName == null) return null;
        int start = xml.indexOf("<" + tagName + ">");
        if (start == -1) {
            // try with namespace prefix: find any ":tagName>"
            start = xml.indexOf(":" + tagName + ">");
            if (start == -1) return null;
            start = xml.indexOf(">", start) + 1;
        } else {
            start += tagName.length() + 2; // skip "<tagName>"
        }
        int end = xml.indexOf("</" + tagName + ">", start);
        if (end == -1) {
            // try with namespace prefix
            end = xml.indexOf("</" , start);
            if (end == -1) return null;
        }
        return xml.substring(start, end).trim();
    }

    /** Minimal XML entity escaping to prevent injection through parameter values. */
    private static String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }
}
