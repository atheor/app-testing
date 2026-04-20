package com.atheor.lambda.publisher;

import com.atheor.common.constant.AppConstants;
import com.atheor.common.model.ScheduleEvent;
import com.atheor.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

/**
 * Publishes a processed {@link ScheduleEvent} to an SNS topic as a JSON message.
 *
 * <p>The SNS topic ARN is resolved at publish time from the
 * {@code SNS_TOPIC_ARN} environment variable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SnsPublisherService {

    private final SnsClient snsClient;

    public void publish(ScheduleEvent event) {
        String topicArn = resolveTopicArn();
        String message  = JsonUtil.toJson(event);

        PublishRequest request = PublishRequest.builder()
                .topicArn(topicArn)
                .message(message)
                .subject("ScheduleEvent:" + event.getScheduleId())
                .messageAttributes(buildMessageAttributes(event))
                .build();

        var response = snsClient.publish(request);
        log.info("Published to SNS [messageId={}, correlationId={}]",
                response.messageId(), event.getCorrelationId());
    }

    private String resolveTopicArn() {
        String arn = System.getenv(AppConstants.ENV_SNS_TOPIC_ARN);
        if (arn == null || arn.isBlank()) {
            throw new IllegalStateException(
                    "Environment variable '" + AppConstants.ENV_SNS_TOPIC_ARN + "' is not set");
        }
        return arn;
    }

    private Map<String, MessageAttributeValue> buildMessageAttributes(ScheduleEvent event) {
        return Map.of(
                AppConstants.HEADER_CORRELATION_ID, MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(event.getCorrelationId())
                        .build(),
                "scheduleId", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(event.getScheduleId())
                        .build()
        );
    }
}
