package com.atheor.lambda.processor;

import com.atheor.backend.entity.OperationEntity;
import com.atheor.backend.entity.ScheduleEntity;
import com.atheor.backend.service.BackendService;
import com.atheor.common.model.EventStatus;
import com.atheor.common.model.ScheduleEvent;
import com.atheor.common.util.JsonUtil;
import com.atheor.lambda.publisher.SnsPublisherService;
import com.atheor.mapping.EventToEntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates the processing of a single {@link ScheduleEvent} arriving from SQS:
 * <ol>
 *   <li>Deserialise JSON → {@link ScheduleEvent}</li>
 *   <li>Persist to PostgreSQL via {@link BackendService}</li>
 *   <li>Publish notification to SNS via {@link SnsPublisherService}</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventProcessorService {

    private final BackendService      backendService;
    private final SnsPublisherService snsPublisher;

    public void process(String messageBody) {
        ScheduleEvent event = JsonUtil.fromJson(messageBody, ScheduleEvent.class);
        log.info("Processing ScheduleEvent [scheduleId={}, correlationId={}]",
                event.getScheduleId(), event.getCorrelationId());

        // 1. Persist
        event.setStatus(EventStatus.PROCESSING);
        ScheduleEntity schedule = EventToEntityMapper.INSTANCE.toScheduleEntity(event);

        if (event.getOperations() != null && !event.getOperations().isEmpty()) {
            List<OperationEntity> ops = EventToEntityMapper.INSTANCE
                    .toOperationEntities(event.getOperations());
            ops.forEach(schedule::addOperation);
        }

        ScheduleEntity saved = backendService.saveSchedule(schedule);
        log.info("Persisted schedule [id={}, scheduleId={}]",
                saved.getId(), saved.getScheduleId());

        // 2. Publish to SNS
        event.setStatus(EventStatus.PROCESSED);
        snsPublisher.publish(event);

        // 3. Update status to PUBLISHED
        backendService.updateStatus(event.getScheduleId(), EventStatus.PUBLISHED);
        log.info("ScheduleEvent fully processed [correlationId={}]", event.getCorrelationId());
    }
}
