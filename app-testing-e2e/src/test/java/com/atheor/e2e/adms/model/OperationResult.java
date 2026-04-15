package com.atheor.e2e.adms.model;

/**
 * Holds the result of a {@code CreateOperation} SOAP call.
 */
public class OperationResult {

    private final String rawResponse;
    private final String operationId;

    public OperationResult(String rawResponse, String operationId) {
        this.rawResponse = rawResponse;
        this.operationId = operationId;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public String getOperationId() {
        return operationId;
    }
}
