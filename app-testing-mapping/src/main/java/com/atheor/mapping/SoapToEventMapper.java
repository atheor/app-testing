package com.atheor.mapping;

import com.atheor.common.model.EventStatus;
import com.atheor.common.model.OperationEvent;
import com.atheor.common.model.ScheduleEvent;
import com.atheor.wsdl.CreateScheduleRequest;
import com.atheor.wsdl.Operation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper
public interface SoapToEventMapper {

    SoapToEventMapper INSTANCE = Mappers.getMapper(SoapToEventMapper.class);

    @Mapping(target = "correlationId",  expression = "java(generateCorrelationId())")
    @Mapping(target = "scheduleId",     source = "scheduleId")
    @Mapping(target = "scheduleName",   source = "scheduleName")
    @Mapping(target = "startDate",      source = "startDate")
    @Mapping(target = "endDate",        source = "endDate")
    @Mapping(target = "operations",     source = "operations.operation")
    @Mapping(target = "status",         expression = "java(com.atheor.common.model.EventStatus.RECEIVED)")
    @Mapping(target = "createdAt",      expression = "java(java.time.Instant.now())")
    ScheduleEvent toEvent(CreateScheduleRequest request);

    @Mapping(target = "operationId",   source = "operationId")
    @Mapping(target = "operationName", source = "operationName")
    @Mapping(target = "operationType", source = "operationType")
    @Mapping(target = "payload",       source = "payload")
    OperationEvent toOperationEvent(Operation operation);

    default List<OperationEvent> toOperationEvents(List<Operation> operations) {
        if (operations == null) {
            return Collections.emptyList();
        }
        return operations.stream()
                .map(this::toOperationEvent)
                .collect(Collectors.toList());
    }

    default String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
