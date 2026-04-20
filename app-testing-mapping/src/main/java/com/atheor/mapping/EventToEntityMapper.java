package com.atheor.mapping;

import com.atheor.backend.entity.OperationEntity;
import com.atheor.backend.entity.ScheduleEntity;
import com.atheor.common.model.EventStatus;
import com.atheor.common.model.OperationEvent;
import com.atheor.common.model.ScheduleEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface EventToEntityMapper {

    EventToEntityMapper INSTANCE = Mappers.getMapper(EventToEntityMapper.class);

    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "scheduleId",    source = "scheduleId")
    @Mapping(target = "scheduleName",  source = "scheduleName")
    @Mapping(target = "startDate",     source = "startDate")
    @Mapping(target = "endDate",       source = "endDate")
    @Mapping(target = "status",        source = "status")
    @Mapping(target = "correlationId", source = "correlationId")
    @Mapping(target = "createdAt",     ignore = true)
    @Mapping(target = "updatedAt",     ignore = true)
    @Mapping(target = "operations",    ignore = true)
    ScheduleEntity toScheduleEntity(ScheduleEvent event);

    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "schedule",      ignore = true)
    @Mapping(target = "operationId",   source = "operationId")
    @Mapping(target = "operationName", source = "operationName")
    @Mapping(target = "operationType", source = "operationType")
    @Mapping(target = "payload",       source = "payload")
    @Mapping(target = "createdAt",     ignore = true)
    OperationEntity toOperationEntity(OperationEvent event);

    List<OperationEntity> toOperationEntities(List<OperationEvent> events);
}
