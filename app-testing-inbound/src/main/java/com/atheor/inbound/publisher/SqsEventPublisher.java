package com.atheor.inbound.publisher;

import com.atheor.common.constant.AppConstants;
import com.atheor.common.model.ScheduleEvent;
import com.atheor.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;

/**
 * Serialises a {@link ScheduleEvent} to JSON and sends it to an SQS queue.
 *
 * <p>The SQS queue URL is resolved from the {@code SQS_QUEUE_URL} environment variable.
 */
@Slf4j
@RequiredArgsConstructor
public class SqsEventPublisher {

    private final SqsClient sqsClient;

    public void publish(ScheduleEvent event) {
        String queueUrl = resolveQueueUrl();
        String messageBody = JsonUtil.toJson(event);

        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .messageAttributes(buildMessageAttributes(event))
                .build();

        var response = sqsClient.sendMessage(request);
        log.info("Message sent to SQS [messageId={}, correlationId={}]",
                response.messageId(), event.getCorrelationId());
    }

    private String resolveQueueUrl() {
        String url = System.getenv(AppConstants.ENV_SQS_QUEUE_URL);
        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                    "Environment variable '" + AppConstants.ENV_SQS_QUEUE_URL + "' is not set");
        }
        return url;
    }

    private Map<String, MessageAttributeValue> buildMessageAttributes(ScheduleEvent event) {
        return Map.of(
                AppConstants.HEADER_CORRELATION_ID, MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(event.getCorrelationId())
                        .build()
        );
    }
}
