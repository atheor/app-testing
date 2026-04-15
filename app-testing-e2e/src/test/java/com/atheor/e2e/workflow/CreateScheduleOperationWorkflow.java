package com.atheor.e2e.workflow;

import com.atheor.e2e.service.InboundSoapServiceClient;
import com.atheor.framework.payload.PayloadFileLoader;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

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

        String scheduleId = extractValueByLocalName(createScheduleResponse, "scheduleId");
        assertNotNull(scheduleId, "CreateSchedule response must contain scheduleId");
        assertFalse(scheduleId.isBlank(), "CreateSchedule response returned empty scheduleId");

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

        String operationId = extractValueByLocalName(createOperationResponse, "operationId");
        assertNotNull(operationId, "CreateOperation response must contain operationId");
        assertFalse(operationId.isBlank(), "CreateOperation response returned empty operationId");

        return new WorkflowResult(scheduleId, operationId, createScheduleResponse, createOperationResponse);
    }

    private void assertHasSoapResponse(String responseXml, String operationName) throws Exception {
        assertNotNull(responseXml, operationName + " returned null response");
        assertFalse(responseXml.isBlank(), operationName + " returned empty response");

        String faultString = extractValueByLocalName(responseXml, "faultstring");
        if (faultString != null && !faultString.isBlank()) {
            fail(operationName + " returned SOAP Fault: " + faultString);
        }

        assertTrue(responseXml.contains("Envelope"), operationName + " response is not a SOAP envelope");
    }

    private String extractValueByLocalName(String xml, String localName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        Document document = factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "//*[local-name()='" + localName + "']/text()";
        String value = (String) xPath.compile(expression).evaluate(document, XPathConstants.STRING);

        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record WorkflowResult(String scheduleId,
                                 String operationId,
                                 String createScheduleResponse,
                                 String createOperationResponse) {
    }
}
