package com.atheor.e2e.adms.model;

/**
 * Holds the result of a {@code CreateSchedule} SOAP call.
 */
public class ScheduleResult {

    private final String rawResponse;
    private final String scheduleId;

    public ScheduleResult(String rawResponse, String scheduleId) {
        this.rawResponse = rawResponse;
        this.scheduleId = scheduleId;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public String getScheduleId() {
        return scheduleId;
    }
}
