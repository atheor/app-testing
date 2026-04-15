package com.atheor.e2e.adms.model;

/**
 * Holds the result of a {@code PrecheckPlannedOrder} SOAP call.
 */
public class PrecheckResult {

    private final String rawResponse;
    private final String status;
    private final String message;

    public PrecheckResult(String rawResponse, String status, String message) {
        this.rawResponse = rawResponse;
        this.status = status;
        this.message = message;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
