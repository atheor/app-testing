package com.atheor.e2e.specs.adms;

import com.atheor.e2e.adms.client.AdmsSoapClient;
import com.atheor.e2e.adms.model.OperationResult;
import com.atheor.e2e.adms.model.ScheduleResult;
import com.atheor.e2e.adms.model.WebVerificationResult;
import com.atheor.e2e.adms.workflow.AdmsCreateScheduleWorkflow;
import com.atheor.framework.web.DriverFactory;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test for the GE ADMS Create Schedule / Create Operation workflow.
 *
 * <p>Execution order:
 * <ol>
 *   <li>{@code CreateSchedule} SOAP — asserts response and extracted {@code scheduleId}</li>
 *   <li>{@code CreateOperation} SOAP — asserts response and extracted {@code operationId}</li>
 *   <li>ADMS portal web verification — asserts schedule and operation are visible in the UI</li>
 * </ol>
 *
 * <p>The workflow is executed once in {@code @BeforeAll}; individual {@code @Test} methods
 * assert on cached results so each step can be reported independently.
 */
@DisplayName("GE ADMS — Create Schedule and Operation Workflow")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdmsCreateScheduleTest {

    private static final String SCHEDULE_NAME  = "Test Schedule";
    private static final String START_DATE     = "2026-04-15T08:00:00";
    private static final String END_DATE       = "2026-04-15T17:00:00";
    private static final String OPERATION_NAME = "Test Operation";
    private static final String DEVICE_ID      = "DEVICE-001";

    private static AdmsCreateScheduleWorkflow workflow;

    @BeforeAll
    static void runWorkflow() throws Exception {
        DriverFactory.initDriver();
        AdmsSoapClient soapClient = new AdmsSoapClient();
        workflow = new AdmsCreateScheduleWorkflow(soapClient, DriverFactory.getDriver());
        workflow.execute(SCHEDULE_NAME, START_DATE, END_DATE, OPERATION_NAME, DEVICE_ID);
    }

    @AfterAll
    static void tearDown() {
        DriverFactory.quit();
    }

    // ---- Step 1: Create Schedule (SOAP) ----

    @Test
    @Order(1)
    @DisplayName("CreateSchedule returns a non-blank SOAP response")
    void createSchedule_responseIsNotBlank() {
        ScheduleResult result = workflow.getScheduleResult();
        assertNotNull(result, "ScheduleResult must not be null");
        assertFalse(result.getRawResponse().isBlank(),
                "CreateSchedule raw response must not be blank");
    }

    @Test
    @Order(2)
    @DisplayName("CreateSchedule response is a valid SOAP envelope")
    void createSchedule_responseIsValidSoapEnvelope() {
        String response = workflow.getScheduleResult().getRawResponse();
        assertAll("CreateSchedule SOAP envelope structure",
                () -> assertTrue(response.contains("Envelope"), "Missing Envelope element"),
                () -> assertTrue(response.contains("Body"),     "Missing Body element")
        );
    }

    @Test
    @Order(3)
    @DisplayName("CreateSchedule response contains a scheduleId")
    void createSchedule_responseContainsScheduleId() {
        ScheduleResult result = workflow.getScheduleResult();
        assertNotNull(result.getScheduleId(),  "scheduleId must not be null");
        assertFalse(result.getScheduleId().isBlank(), "scheduleId must not be blank");
    }

    // ---- Step 2: Create Operation (SOAP) ----

    @Test
    @Order(4)
    @DisplayName("CreateOperation returns a non-blank SOAP response")
    void createOperation_responseIsNotBlank() {
        OperationResult result = workflow.getOperationResult();
        assertNotNull(result, "OperationResult must not be null");
        assertFalse(result.getRawResponse().isBlank(),
                "CreateOperation raw response must not be blank");
    }

    @Test
    @Order(5)
    @DisplayName("CreateOperation response is a valid SOAP envelope")
    void createOperation_responseIsValidSoapEnvelope() {
        String response = workflow.getOperationResult().getRawResponse();
        assertAll("CreateOperation SOAP envelope structure",
                () -> assertTrue(response.contains("Envelope"), "Missing Envelope element"),
                () -> assertTrue(response.contains("Body"),     "Missing Body element")
        );
    }

    @Test
    @Order(6)
    @DisplayName("CreateOperation response contains an operationId")
    void createOperation_responseContainsOperationId() {
        OperationResult result = workflow.getOperationResult();
        assertNotNull(result.getOperationId(),  "operationId must not be null");
        assertFalse(result.getOperationId().isBlank(), "operationId must not be blank");
    }

    // ---- Step 3: Portal Web Verification ----

    @Test
    @Order(7)
    @DisplayName("Portal shows the created schedule in the schedule list")
    void portal_scheduleIsVisible() {
        WebVerificationResult result = workflow.getWebVerificationResult();
        assertNotNull(result, "WebVerificationResult must not be null");
        assertTrue(result.isScheduleVisible(),
                "Expected schedule '" + SCHEDULE_NAME + "' to be visible in the ADMS portal");
    }

    @Test
    @Order(8)
    @DisplayName("Portal shows the created operation under the schedule")
    void portal_operationIsVisible() {
        WebVerificationResult result = workflow.getWebVerificationResult();
        assertTrue(result.isOperationVisible(),
                "Expected operation '" + OPERATION_NAME + "' to be visible under schedule '"
                        + SCHEDULE_NAME + "' in the ADMS portal");
    }
}
