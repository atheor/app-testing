package com.atheor.lambda.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.atheor.lambda.LambdaApplication;
import com.atheor.lambda.processor.EventProcessorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ApplicationContext;

/**
 * Lambda handler for SQS-triggered events.
 *
 * <p>Flow: SQS → {@code handleRequest} → {@link EventProcessorService#process}
 * → persist to PostgreSQL → publish to SNS.
 *
 * <p>The Spring {@link ApplicationContext} is initialised exactly once (static field)
 * and reused across all warm Lambda invocations, significantly reducing
 * per-invocation overhead after the first cold start.
 */
@Slf4j
public class SqsConsumerLambdaHandler implements RequestHandler<SQSEvent, String> {

    /**
     * Lazily initialised Spring context — shared across warm invocations.
     * Uses double-checked locking for thread safety.
     */
    private static volatile ApplicationContext springContext;

    @Override
    public String handleRequest(SQSEvent event, Context lambdaContext) {
        log.info("SQS Consumer Lambda invoked [requestId={}, messageCount={}]",
                lambdaContext.getAwsRequestId(), event.getRecords().size());

        EventProcessorService processor = getSpringContext().getBean(EventProcessorService.class);

        int processed = 0;
        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {
                processor.process(message.getBody());
                processed++;
            } catch (Exception e) {
                // Log and re-throw so SQS can re-drive to the DLQ
                log.error("Failed to process message [messageId={}]", message.getMessageId(), e);
                throw new RuntimeException("Message processing failed — triggering SQS retry", e);
            }
        }

        String result = "Processed " + processed + "/" + event.getRecords().size() + " messages";
        log.info(result);
        return result;
    }

    // ------------------------------------------------------------------
    // Spring context initialisation
    // ------------------------------------------------------------------

    private static ApplicationContext getSpringContext() {
        if (springContext == null) {
            synchronized (SqsConsumerLambdaHandler.class) {
                if (springContext == null) {
                    log.info("Initialising Spring ApplicationContext (cold start)");
                    SpringApplication app = new SpringApplication(LambdaApplication.class);
                    app.setWebApplicationType(WebApplicationType.NONE);
                    springContext = app.run();
                    log.info("Spring ApplicationContext ready");
                }
            }
        }
        return springContext;
    }
}
