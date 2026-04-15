package com.atheor.e2e.adms;

import com.atheor.e2e.adms.client.AdmsSoapClient;
import com.atheor.e2e.adms.model.OperationResult;
import com.atheor.e2e.adms.model.PrecheckResult;
import com.atheor.e2e.adms.model.ScheduleResult;
import com.atheor.e2e.adms.workflow.AdmsWorkflow;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GE ADMS — Planned Order Workflow")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdmsWorkflowTest {

    private static AdmsWorkflow workflow;

    @BeforeAll
    static void runWorkflow() throws Exception {
        AdmsSoapClient client = new AdmsSoapClient();
        workflow = new AdmsWorkflow(client);

        workflow.execute(
                "Test Schedule",
                "2026-04-14T08:00:00",
                "2026-04-14T17:00:00",
                "Test Operation",
                "DEVICE-001"
        );
    }

    // ---- Step 1: Create Schedule ----

    @Test
    @Order(1)
    @DisplayName("CreateSchedule returns a non-blank response")
    void createSchedule_responseIsNotBlank() {
        ScheduleResult result = workflow.getScheduleResult();
        assertNotNull(result, "ScheduleResult must not be null");
        assertFalse(result.getRawResponse().isBlank(),
                "CreateSchedule raw response must not be blank");
    }

    @Test
    @Order(2)
    @DisplayName("CreateSchedule response contains a scheduleId")
    void createSchedule_responseContainsScheduleId() {
        ScheduleResult result = workflow.getScheduleResult();
        assertNotNull(result.getScheduleId(),
                "scheduleId must be extractable from CreateSchedule response");
        assertFalse(result.getScheduleId().isBlank(),
                "scheduleId must not be blank");
    }

    @Test
    @Order(3)
    @DisplayName("CreateSchedule response is a valid SOAP envelope")
    void createSchedule_responseIsValidSoapEnvelope() {
        String response = workflow.getScheduleResult().getRawResponse();
        assertTrue(response.contains("Envelope"),
                "CreateSchedule response does not contain SOAP Envelope");
        assertTrue(response.contains("Body"),
                "CreateSchedule response does not contain SOAP Body");
    }

    // ---- Step 2: Create Operation ----

    @Test
    @Order(4)
    @DisplayName("CreateOperation returns a non-blank response")
    void createOperation_responseIsNotBlank() {
        OperationResult result = workflow.getOperationResult();
        assertNotNull(result, "OperationResult must not be null");
        assertFalse(result.getRawResponse().isBlank(),
                "CreateOperation raw response must not be blank");
    }

    @Test
    @Order(5)
    @DisplayName("CreateOperation response contains an operationId")
    void createOperation_responseContainsOperationId() {
        OperationResult result = workflow.getOperationResult();
        assertNotNull(result.getOperationId(),
                "operationId must be extractable from CreateOperation response");
        assertFalse(result.getOperationId().isBlank(),
                "operationId must not be blank");
    }

    @Test
    @Order(6)
    @DisplayName("CreateOperation response is a valid SOAP envelope")
    void createOperation_responseIsValidSoapEnvelope() {
        String response = workflow.getOperationResult().getRawResponse();
        assertTrue(response.contains("Envelope"),
                "CreateOperation response does not contain SOAP Envelope");
        assertTrue(response.contains("Body"),
                "CreateOperation response does not contain SOAP Body");
    }

    // ---- Step 3: Precheck Planned Order ----

    @Test
    @Order(7)
    @DisplayName("PrecheckPlannedOrder returns a non-blank response")
    void precheckPlannedOrder_responseIsNotBlank() {
        PrecheckResult result = workflow.getPrecheckResult();
        assertNotNull(result, "PrecheckResult must not be null");
        assertFalse(result.getRawResponse().isBlank(),
                "PrecheckPlannedOrder raw response must not be blank");
    }

    @Test
    @Order(8)
    @DisplayName("PrecheckPlannedOrder returns SUCCESS status")
    void precheckPlannedOrder_returnsSuccessStatus() {
        PrecheckResult result = workflow.getPrecheckResult();
        assertEquals("SUCCESS", result.getStatus(),
                "Expected precheck status to be SUCCESS. Message: " + result.getMessage());
    }

    @Test
    @Order(9)
    @DisplayName("PrecheckPlannedOrder response is a valid SOAP envelope")
    void precheckPlannedOrder_responseIsValidSoapEnvelope() {
        String response = workflow.getPrecheckResult().getRawResponse();
        assertTrue(response.contains("Envelope"),
                "PrecheckPlannedOrder response does not contain SOAP Envelope");
        assertTrue(response.contains("Body"),
                "PrecheckPlannedOrder response does not contain SOAP Body");
    }
}
