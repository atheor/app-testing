package com.atheor.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScheduleEvent {

    private String correlationId;
    private String scheduleId;
    private String scheduleName;
    private String startDate;
    private String endDate;
    private List<OperationEvent> operations;
    private EventStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;
}
