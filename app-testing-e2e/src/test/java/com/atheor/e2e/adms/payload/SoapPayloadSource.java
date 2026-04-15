package com.atheor.e2e.adms.payload;

/**
 * Common contract for SOAP payload sources.
 *
 * <p>Two built-in implementations are provided:
 * <ul>
 *   <li>{@link FilePayloadLoader} — loads a full SOAP envelope from an XML template file on the
 *       classpath and substitutes {@code ${token}} placeholders with supplied values.</li>
 *   <li>{@link CreateSchedulePayload} / {@link CreateOperationPayload} — fluent builders that
 *       construct the envelope programmatically from typed fields.</li>
 * </ul>
 *
 * <p>Shared WS-Security and SOAP namespace constants are defined here so all implementations
 * reference the same values without duplication.
 */
public interface SoapPayloadSource {

    String SOAPENV_NS = "http://schemas.xmlsoap.org/soap/envelope/";

    String WSSE_NS =
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

    String PASSWORD_TEXT_TYPE =
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText";

    /**
     * Returns the complete SOAP 1.1 envelope XML as a string, ready to be sent
     * via {@link com.atheor.framework.soap.SoapClient}.
     */
    String toEnvelope();
}
