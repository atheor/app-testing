package com.atheor.e2e.adms.model;

/**
 * Holds the outcome of the web-portal verification step in the workflow.
 */
public class WebVerificationResult {

    private final boolean scheduleVisible;
    private final boolean operationVisible;

    public WebVerificationResult(boolean scheduleVisible, boolean operationVisible) {
        this.scheduleVisible = scheduleVisible;
        this.operationVisible = operationVisible;
    }

    /** {@code true} if the schedule name was found in the schedules list page. */
    public boolean isScheduleVisible() {
        return scheduleVisible;
    }

    /** {@code true} if the operation name was found in the schedule detail page. */
    public boolean isOperationVisible() {
        return operationVisible;
    }
}
