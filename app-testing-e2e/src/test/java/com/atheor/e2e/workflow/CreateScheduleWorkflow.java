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
import static org.junit.jupiter.api.Assertions.fail;

/**
 * End-to-end workflow for the {@code CreateSchedule} SOAP operation.
 *
 * <p>Flow:
 * <ol>
 *   <li>Build a {@code CreateScheduleRequest} envelope from the classpath template.</li>
 *   <li>POST it to the Inbound Lambda via API Gateway ({@code POST /soap/schedule}).</li>
 *   <li>Assert the response is a well-formed {@code CreateScheduleResponse} with
 *       {@code status=CREATED} and a non-blank {@code correlationId}.</li>
 * </ol>
 */
public class CreateScheduleWorkflow {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final InboundSoapServiceClient soapServiceClient;

    public CreateScheduleWorkflow(InboundSoapServiceClient soapServiceClient) {
        this.soapServiceClient = soapServiceClient;
    }

    /**
     * Executes the full CreateSchedule workflow and returns the parsed result.
     *
     * @return {@link WorkflowResult} containing the fields extracted from the SOAP response
     * @throws Exception on transport or XML parsing failure
     */
    public WorkflowResult execute() throws Exception {
        String scheduleId = UUID.randomUUID().toString();
        String correlation = scheduleId.substring(0, 8);
        LocalDateTime start = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(1);
        LocalDateTime end   = start.plusHours(2);

        String requestPayload = PayloadFileLoader
                .fromClasspath("payloads/inbound/create_schedule.xml")
                .with("scheduleId",   scheduleId)
                .with("scheduleName", "schedule-" + correlation)
                .with("startDate",    start.format(ISO))
                .with("endDate",      end.format(ISO))
                .build();

        String responseXml = soapServiceClient.createSchedule(requestPayload);
        assertValidSoapResponse(responseXml);

        String returnedScheduleId = extractRequired(responseXml, "scheduleId");
        String status             = extractRequired(responseXml, "status");
        String returnedCorrelId   = extractRequired(responseXml, "correlationId");

        assertEquals("CREATED", status,
                "CreateSchedule response status should be CREATED");

        return new WorkflowResult(returnedScheduleId, status, returnedCorrelId, responseXml);
    }

    // ------------------------------------------------------------------
    // Assertion helpers
    // ------------------------------------------------------------------

    private static void assertValidSoapResponse(String responseXml) throws Exception {
        assertNotNull(responseXml, "CreateSchedule returned null response");
        assertFalse(responseXml.isBlank(), "CreateSchedule returned empty response");

        String faultString = XmlValueExtractor.extractValueByLocalName(responseXml, "faultstring");
        if (faultString != null && !faultString.isBlank()) {
            fail("CreateSchedule returned SOAP Fault: " + faultString);
        }
    }

    private static String extractRequired(String responseXml, String localName) throws Exception {
        String value = XmlValueExtractor.extractValueByLocalName(responseXml, localName);
        assertNotNull(value, "Response is missing element <" + localName + ">");
        assertFalse(value.isBlank(), "Response element <" + localName + "> must not be blank");
        return value;
    }

    // ------------------------------------------------------------------
    // Result record
    // ------------------------------------------------------------------

    /**
     * Holds the fields extracted from a successful {@code CreateScheduleResponse}.
     *
     * @param scheduleId    schedule identifier echoed from the request
     * @param status        should always be {@code "CREATED"}
     * @param correlationId server-assigned tracing identifier
     * @param responseXml   raw SOAP response envelope (for further assertions)
     */
    public record WorkflowResult(
            String scheduleId,
            String status,
            String correlationId,
            String responseXml) {}
}

