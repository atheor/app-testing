package com.atheor.e2e.workflow;

import com.atheor.e2e.service.InboundSoapServiceClient;
import com.atheor.framework.payload.PayloadFileLoader;
import com.atheor.framework.soap.XmlValueExtractor;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Behavior workflow:
 * 1) CreateSchedule
 * 2) Wait for response and extract scheduleId
 * 3) CreateOperation using scheduleId
 */
public class CreateScheduleOperationWorkflow {

    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final InboundSoapServiceClient soapServiceClient;
    private final String username;
    private final String password;

    public CreateScheduleOperationWorkflow(InboundSoapServiceClient soapServiceClient,
                                           String username,
                                           String password) {
        this.soapServiceClient = soapServiceClient;
        this.username = username;
        this.password = password;
    }

    public WorkflowResult execute() throws Exception {
        String correlation = UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime start = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(1);
        LocalDateTime end = start.plusHours(2);

        ScheduleResult scheduleResult = createSchedule(correlation, start, end);
        OperationResult operationResult = createOperation(correlation, scheduleResult.scheduleId());

        return new WorkflowResult(
            scheduleResult.scheduleId(),
            operationResult.operationId(),
            scheduleResult.responseXml(),
            operationResult.responseXml());
        }

        private ScheduleResult createSchedule(String correlation,
                          LocalDateTime start,
                          LocalDateTime end) throws Exception {
        String schedulePayload = PayloadFileLoader
                .fromClasspath("payloads/adms/create_schedule.xml")
                .with("username", username)
                .with("password", password)
                .with("scheduleName", "schedule-" + correlation)
                .with("startDate", start.format(ISO_DATE_TIME))
                .with("endDate", end.format(ISO_DATE_TIME))
                .build();

        String createScheduleResponse = soapServiceClient.createSchedule(schedulePayload);
        assertHasSoapResponse(createScheduleResponse, "CreateSchedule");

        String scheduleId = getResponsePropertyValue(createScheduleResponse, "scheduleId", "CreateSchedule");

        return new ScheduleResult(scheduleId, createScheduleResponse);
        }

        private OperationResult createOperation(String correlation,
                            String scheduleId) throws Exception {
        String operationPayload = PayloadFileLoader
                .fromClasspath("payloads/adms/create_operation.xml")
                .with("username", username)
                .with("password", password)
                .with("scheduleId", scheduleId)
                .with("operationName", "operation-" + correlation)
                .with("deviceId", "device-" + correlation)
                .build();

        String createOperationResponse = soapServiceClient.createOperation(operationPayload);
        assertHasSoapResponse(createOperationResponse, "CreateOperation");

        String operationId = getResponsePropertyValue(createOperationResponse, "operationId", "CreateOperation");

        return new OperationResult(operationId, createOperationResponse);
    }

    private void assertHasSoapResponse(String responseXml, String operationName) throws Exception {
        assertNotNull(responseXml, operationName + " returned null response");
        assertFalse(responseXml.isBlank(), operationName + " returned empty response");

        String faultString = XmlValueExtractor.extractValueByLocalName(responseXml, "faultstring");
        if (faultString != null && !faultString.isBlank()) {
            fail(operationName + " returned SOAP Fault: " + faultString);
        }

        assertTrue(responseXml.contains("Envelope"), operationName + " response is not a SOAP envelope");
    }

    public String getResponsePropertyValue(String responseXml,
                                           String propertyLocalName,
                                           String operationName) throws Exception {
        String propertyValue = XmlValueExtractor.extractValueByLocalName(responseXml, propertyLocalName);
        assertNotNull(propertyValue, operationName + " response must contain " + propertyLocalName);
        assertFalse(propertyValue.isBlank(), operationName + " response returned empty " + propertyLocalName);
        return propertyValue;
    }

    public void assertResponsePropertyValue(String responseXml,
                                            String propertyLocalName,
                                            String expectedValue,
                                            String operationName) throws Exception {
        String actualValue = getResponsePropertyValue(responseXml, propertyLocalName, operationName);
        assertEquals(expectedValue, actualValue,
                operationName + " response " + propertyLocalName + " does not match expected value");
    }

    private record ScheduleResult(String scheduleId, String responseXml) {
    }

    private record OperationResult(String operationId, String responseXml) {
    }

    public record WorkflowResult(String scheduleId,
                                 String operationId,
                                 String createScheduleResponse,
                                 String createOperationResponse) {
    }
}
