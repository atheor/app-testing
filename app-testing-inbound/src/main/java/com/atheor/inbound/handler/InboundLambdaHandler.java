package com.atheor.inbound.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.atheor.common.constant.AppConstants;
import com.atheor.common.model.ScheduleEvent;
import com.atheor.inbound.config.AwsClientConfig;
import com.atheor.inbound.publisher.SqsEventPublisher;
import com.atheor.mapping.SoapToEventMapper;
import com.atheor.soap.builder.SoapResponseBuilder;
import com.atheor.soap.parser.SoapMessageParser;
import com.atheor.wsdl.CreateScheduleRequest;
import com.atheor.wsdl.CreateScheduleResponse;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Entry point for the Inbound Lambda.
 *
 * <p>Flow: API Gateway → {@code handleRequest} → parse SOAP → map to event
 * → publish to SQS → return SOAP response.
 *
 * <p>Heavy objects ({@link SoapMessageParser}, {@link SqsEventPublisher}) are
 * initialised once in the static block and reused across warm invocations.
 */
@Slf4j
public class InboundLambdaHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final SoapMessageParser  SOAP_PARSER   = new SoapMessageParser();
    private static final SoapResponseBuilder SOAP_BUILDER = new SoapResponseBuilder();
    private static final SqsEventPublisher  SQS_PUBLISHER =
            new SqsEventPublisher(AwsClientConfig.sqsClient());

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        String correlationId = "unknown";
        try {
            log.info("Inbound SOAP request received [requestId={}]", context.getAwsRequestId());

            // 1. Validate Content-Type — SOAP/HTTP binding spec requires text/xml
            String contentType = getHeaderCaseInsensitive(
                    input.getHeaders(), AppConstants.HEADER_CONTENT_TYPE);
            if (contentType == null || !contentType.toLowerCase().contains("text/xml")) {
                log.warn("Rejected request: unsupported Content-Type [{}]", contentType);
                return soapFaultResponse(415, "soap:Client",
                        "Unsupported Media Type: Content-Type must be text/xml");
            }

            // 2. Log SOAPAction (optional per spec; useful for tracing and future routing)
            String soapAction = getHeaderCaseInsensitive(
                    input.getHeaders(), AppConstants.HEADER_SOAP_ACTION);
            log.info("SOAPAction: {}", soapAction);

            // 3. Decode body (API Gateway may base64-encode if binary_media_types is set)
            String soapBody = decodeBody(input);
            if (soapBody == null || soapBody.isBlank()) {
                log.warn("Empty request body received");
                return soapFaultResponse(400, "soap:Client", "Request body is empty");
            }

            // 4. Parse SOAP
            CreateScheduleRequest soapRequest = SOAP_PARSER.parseCreateScheduleRequest(soapBody);

            // 5. Map to domain event (correlation ID generated here)
            ScheduleEvent event = SoapToEventMapper.INSTANCE.toEvent(soapRequest);
            correlationId = event.getCorrelationId();
            log.info("Mapped SOAP to ScheduleEvent [scheduleId={}, correlationId={}]",
                    event.getScheduleId(), correlationId);

            // 6. Publish to SQS
            SQS_PUBLISHER.publish(event);
            log.info("Event published to SQS [correlationId={}]", correlationId);

            // 7. Return success SOAP response
            CreateScheduleResponse soapResponse = CreateScheduleResponse.builder()
                    .scheduleId(event.getScheduleId())
                    .status(AppConstants.DEFAULT_STATUS_CREATED)
                    .correlationId(correlationId)
                    .message("Schedule received and queued for processing")
                    .build();

            return soapSuccessResponse(SOAP_BUILDER.buildCreateScheduleResponse(soapResponse));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid SOAP request [correlationId={}]: {}", correlationId, e.getMessage());
            return soapFaultResponse(400, "soap:Client", e.getMessage());
        } catch (Exception e) {
            log.error("Internal error processing SOAP request [correlationId={}]", correlationId, e);
            return soapFaultResponse(500, "soap:Server", "Internal server error");
        }
    }

    // ------------------------------------------------------------------
    // Response helpers
    // ------------------------------------------------------------------

    private APIGatewayProxyResponseEvent soapSuccessResponse(String soapXml) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(Map.of(AppConstants.HEADER_CONTENT_TYPE, AppConstants.CONTENT_TYPE_SOAP))
                .withBody(soapXml);
    }

    private APIGatewayProxyResponseEvent soapFaultResponse(int status, String code, String message) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(status)
                .withHeaders(Map.of(AppConstants.HEADER_CONTENT_TYPE, AppConstants.CONTENT_TYPE_SOAP))
                .withBody(SOAP_BUILDER.buildFaultResponse(code, message));
    }

    // ------------------------------------------------------------------
    // Utility helpers
    // ------------------------------------------------------------------

    /**
     * Decodes the request body. API Gateway sets {@code isBase64Encoded=true} when
     * the content type is listed in {@code binary_media_types}; this is a defensive
     * guard for forward-compatibility.
     */
    private static String decodeBody(APIGatewayProxyRequestEvent input) {
        String body = input.getBody();
        if (body == null) return null;
        if (Boolean.TRUE.equals(input.getIsBase64Encoded())) {
            return new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8);
        }
        return body;
    }

    /**
     * Case-insensitive header lookup. API Gateway normalises header names to
     * lowercase in the Lambda event, but this guard handles any deviations.
     */
    private static String getHeaderCaseInsensitive(Map<String, String> headers, String name) {
        if (headers == null || headers.isEmpty()) return null;
        return headers.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
