package com.atheor.e2e.service;

import com.atheor.framework.soap.SoapClient;

import java.io.IOException;

/**
 * Small service adapter for ADMS inbound SOAP operations.
 */
public class InboundSoapServiceClient {

    private static final String CREATE_SCHEDULE_ACTION = "wpm/CreateSchedule";
    private static final String CREATE_OPERATION_ACTION = "wpm/CreateOperation";

    private final String endpoint;

    public InboundSoapServiceClient(String endpoint) {
        this.endpoint = endpoint;
    }

    public String createSchedule(String soapEnvelope) throws IOException {
        return new SoapClient(CREATE_SCHEDULE_ACTION).sendRequest(endpoint, soapEnvelope);
    }

    public String createOperation(String soapEnvelope) throws IOException {
        return new SoapClient(CREATE_OPERATION_ACTION).sendRequest(endpoint, soapEnvelope);
    }
}
