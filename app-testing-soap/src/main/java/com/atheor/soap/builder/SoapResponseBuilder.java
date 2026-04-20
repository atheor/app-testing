package com.atheor.soap.builder;

import com.atheor.wsdl.CreateScheduleResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;

import java.io.StringWriter;

/**
 * Builds well-formed SOAP 1.1 response envelopes from typed JAXB objects.
 */
@Slf4j
public class SoapResponseBuilder {

    private static final String SOAP_ENVELOPE_OPEN =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
            "<soap:Body>";

    private static final String SOAP_ENVELOPE_CLOSE =
            "</soap:Body></soap:Envelope>";

    private static final JAXBContext JAXB_CTX;

    static {
        try {
            JAXB_CTX = JAXBContext.newInstance(CreateScheduleResponse.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError("Failed to initialise JAXBContext: " + e.getMessage());
        }
    }

    /**
     * Wraps a {@link CreateScheduleResponse} inside a SOAP 1.1 envelope.
     *
     * @param response the typed response object
     * @return SOAP XML string
     */
    public String buildCreateScheduleResponse(CreateScheduleResponse response) {
        log.debug("Building SOAP CreateScheduleResponse [scheduleId={}]", response.getScheduleId());

        try {
            Marshaller marshaller = JAXB_CTX.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true); // omit <?xml?> from inner fragment

            StringWriter writer = new StringWriter();
            marshaller.marshal(response, writer);

            return SOAP_ENVELOPE_OPEN + writer + SOAP_ENVELOPE_CLOSE;
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to marshal SOAP CreateScheduleResponse", e);
        }
    }

    /**
     * Builds a SOAP 1.1 fault envelope.
     *
     * @param faultCode   the fault code (e.g. {@code soap:Client}, {@code soap:Server})
     * @param faultString human-readable fault description
     * @return SOAP fault XML string
     */
    public String buildFaultResponse(String faultCode, String faultString) {
        // Sanitise faultString to prevent XML injection
        String safe = faultString == null ? "" : faultString
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
               "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
               "<soap:Body>" +
               "<soap:Fault>" +
               "<faultcode>" + faultCode + "</faultcode>" +
               "<faultstring>" + safe + "</faultstring>" +
               "</soap:Fault>" +
               "</soap:Body>" +
               "</soap:Envelope>";
    }
}
