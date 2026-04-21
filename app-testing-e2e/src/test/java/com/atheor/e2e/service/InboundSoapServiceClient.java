package com.atheor.e2e.service;

import com.atheor.framework.soap.SoapClient;

import java.io.IOException;

/**
 * Service adapter for the Inbound Lambda SOAP endpoint.
 *
 * <p>Maps to the single {@code CreateSchedule} operation exposed at
 * {@code POST /soap/schedule} through API Gateway.
 */
public class InboundSoapServiceClient {

    /** SOAPAction value as defined in AppConstants.CREATE_SCHEDULE_ACTION. */
    private static final String CREATE_SCHEDULE_ACTION = "http://atheor.com/schedule/createSchedule";

    /** Full API Gateway URL including the resource path, e.g.
     *  {@code http://localhost:4566/restapis/{id}/local/_user_request_/soap/schedule}. */
    private final String endpoint;

    public InboundSoapServiceClient(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Sends a {@code CreateSchedule} SOAP request and returns the raw response XML.
     *
     * @param soapEnvelope full SOAP 1.1 envelope
     * @return response body as a string (SOAP response or SOAP fault)
     * @throws IOException if the HTTP transport fails
     */
    public String createSchedule(String soapEnvelope) throws IOException {
        return new SoapClient(CREATE_SCHEDULE_ACTION).sendRequest(endpoint, soapEnvelope);
    }
}
