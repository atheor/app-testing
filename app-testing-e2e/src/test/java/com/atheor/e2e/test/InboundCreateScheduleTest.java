package com.atheor.e2e.test;

import com.atheor.e2e.service.InboundSoapServiceClient;
import com.atheor.e2e.workflow.CreateScheduleWorkflow;
import com.atheor.framework.config.ConfigManager;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * End-to-end test for the {@code CreateSchedule} SOAP operation.
 *
 * <p>Requires a running environment (local via LocalStack or AWS).
 * Configure {@code inbound.endpoint} in {@code test.properties} or pass it as a
 * system property ({@code -Dinbound.endpoint=...}) before running this test.
 *
 * <p>The test is skipped automatically when the endpoint property is absent or
 * still holds the placeholder value, so the CI build stays green on machines
 * that have no deployed environment.
 */
class InboundCreateScheduleTest {

    @Test
    void shouldCreateScheduleAndReceiveCreatedStatus() throws Exception {
        String endpoint = ConfigManager.get("inbound.endpoint");

        Assumptions.assumeTrue(endpoint != null && !endpoint.isBlank(),
                "Set inbound.endpoint in test.properties or -Dinbound.endpoint");
        Assumptions.assumeFalse(endpoint.contains("your-api-gateway"),
                "Configure a real inbound.endpoint before running this test");

        InboundSoapServiceClient serviceClient = new InboundSoapServiceClient(endpoint);
        CreateScheduleWorkflow workflow = new CreateScheduleWorkflow(serviceClient);

        CreateScheduleWorkflow.WorkflowResult result = workflow.execute();

        assertNotNull(result.scheduleId(), "Response scheduleId must not be null");
        assertFalse(result.scheduleId().isBlank(), "Response scheduleId must not be blank");

        assertEquals("CREATED", result.status(),
                "Response status should be CREATED");

        assertNotNull(result.correlationId(), "Response correlationId must not be null");
        assertFalse(result.correlationId().isBlank(), "Response correlationId must not be blank");
    }
}

