package com.atheor.e2e.test;

import com.atheor.e2e.service.InboundSoapServiceClient;
import com.atheor.e2e.workflow.CreateScheduleOperationWorkflow;
import com.atheor.framework.config.ConfigManager;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InboundCreateScheduleCreateOperationTest {

    @Test
    void shouldCreateScheduleThenCreateOperation() throws Exception {
        String endpoint = ConfigManager.get("adms.endpoint");
        String username = ConfigManager.get("adms.username");
        String password = ConfigManager.get("adms.password");

        Assumptions.assumeTrue(endpoint != null && !endpoint.isBlank(),
                "Set adms.endpoint in test.properties or -Dadms.endpoint");
        Assumptions.assumeFalse(endpoint.contains("your-adms-host"),
            "Configure a real adms.endpoint before running this test");

        InboundSoapServiceClient serviceClient = new InboundSoapServiceClient(endpoint);
        CreateScheduleOperationWorkflow workflow =
                new CreateScheduleOperationWorkflow(serviceClient, username, password);

        CreateScheduleOperationWorkflow.WorkflowResult result = workflow.execute();

        assertNotNull(result.scheduleId());
        assertFalse(result.scheduleId().isBlank());
        assertNotNull(result.operationId());
        assertFalse(result.operationId().isBlank());
    }
}
