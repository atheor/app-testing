package com.atheor.e2e.adms.workflow;

import com.atheor.e2e.adms.client.AdmsSoapClient;
import com.atheor.e2e.adms.model.OperationResult;
import com.atheor.e2e.adms.model.PrecheckResult;
import com.atheor.e2e.adms.model.ScheduleResult;

import java.io.IOException;

/**
 * Orchestrates the GE ADMS three-step SOAP workflow:
 *
 * <ol>
 *   <li>Create Schedule — obtains a {@code scheduleId}</li>
 *   <li>Create Operation — uses the {@code scheduleId}, obtains an {@code operationId}</li>
 *   <li>Precheck Planned Order — uses the {@code operationId} to validate the order</li>
 * </ol>
 *
 * <p>Call {@link #execute} to run the full workflow, then retrieve results via the
 * individual getters. This class contains no assertions; assertion logic belongs in
 * the test class.
 */
public class AdmsWorkflow {

    private final AdmsSoapClient client;

    private ScheduleResult scheduleResult;
    private OperationResult operationResult;
    private PrecheckResult precheckResult;

    public AdmsWorkflow(AdmsSoapClient client) {
        this.client = client;
    }

    /**
     * Executes the full three-step ADMS workflow.
     *
     * @param scheduleName  human-readable name for the new schedule
     * @param startDate     start date/time in ISO-8601 format, e.g. {@code "2026-04-14T08:00:00"}
     * @param endDate       end date/time in ISO-8601 format, e.g. {@code "2026-04-14T17:00:00"}
     * @param operationName human-readable name for the switching operation
     * @param deviceId      network device identifier used in both CreateOperation and PrecheckPlannedOrder
     * @throws IOException          if any SOAP call fails at the transport level
     * @throws IllegalStateException if a required ID cannot be extracted from a response
     */
    public void execute(String scheduleName, String startDate, String endDate,
                        String operationName, String deviceId) throws IOException {

        // Step 1: Create Schedule
        String createScheduleResponse = client.createSchedule(scheduleName, startDate, endDate);
        String scheduleId = client.extractElement(createScheduleResponse, "scheduleId");
        if (scheduleId == null || scheduleId.isBlank()) {
            throw new IllegalStateException(
                    "CreateSchedule did not return a scheduleId. Response: " + createScheduleResponse);
        }
        scheduleResult = new ScheduleResult(createScheduleResponse, scheduleId);

        // Step 2: Create Operation (depends on scheduleId from Step 1)
        String createOperationResponse = client.createOperation(scheduleId, operationName, deviceId);
        String operationId = client.extractElement(createOperationResponse, "operationId");
        if (operationId == null || operationId.isBlank()) {
            throw new IllegalStateException(
                    "CreateOperation did not return an operationId. Response: " + createOperationResponse);
        }
        operationResult = new OperationResult(createOperationResponse, operationId);

        // Step 3: Precheck Planned Order (depends on operationId from Step 2)
        String precheckResponse = client.precheckPlannedOrder(operationId, deviceId);
        String status  = client.extractElement(precheckResponse, "status");
        String message = client.extractElement(precheckResponse, "message");
        precheckResult = new PrecheckResult(precheckResponse, status, message);
    }

    /**
     * Executes the workflow using pre-built SOAP envelopes for CreateSchedule/CreateOperation.
     *
     * <p>The operation payload can include a {@code ${scheduleId}} placeholder that is replaced
     * with the ID extracted from the CreateSchedule response.</p>
     */
    public void executeWithPayloads(String createScheduleEnvelope,
                                    String createOperationEnvelopeTemplate,
                                    String deviceId) throws IOException {

        String createScheduleResponse = client.createScheduleFromEnvelope(createScheduleEnvelope);
        String scheduleId = client.extractElement(createScheduleResponse, "scheduleId");
        if (scheduleId == null || scheduleId.isBlank()) {
            throw new IllegalStateException(
                    "CreateSchedule did not return a scheduleId. Response: " + createScheduleResponse);
        }
        scheduleResult = new ScheduleResult(createScheduleResponse, scheduleId);

        String createOperationEnvelope = createOperationEnvelopeTemplate.replace("${scheduleId}", scheduleId);
        String createOperationResponse = client.createOperationFromEnvelope(createOperationEnvelope);
        String operationId = client.extractElement(createOperationResponse, "operationId");
        if (operationId == null || operationId.isBlank()) {
            throw new IllegalStateException(
                    "CreateOperation did not return an operationId. Response: " + createOperationResponse);
        }
        operationResult = new OperationResult(createOperationResponse, operationId);

        String precheckResponse = client.precheckPlannedOrder(operationId, deviceId);
        String status  = client.extractElement(precheckResponse, "status");
        String message = client.extractElement(precheckResponse, "message");
        precheckResult = new PrecheckResult(precheckResponse, status, message);
    }

    // ---- Result accessors ----

    /** Returns the result of Step 1 (CreateSchedule), or {@code null} if not yet executed. */
    public ScheduleResult getScheduleResult() {
        return scheduleResult;
    }

    /** Returns the result of Step 2 (CreateOperation), or {@code null} if not yet executed. */
    public OperationResult getOperationResult() {
        return operationResult;
    }

    /** Returns the result of Step 3 (PrecheckPlannedOrder), or {@code null} if not yet executed. */
    public PrecheckResult getPrecheckResult() {
        return precheckResult;
    }
}
