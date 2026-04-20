package com.atheor.backend.repository;

import com.atheor.backend.entity.OperationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OperationRepository extends JpaRepository<OperationEntity, UUID> {

    List<OperationEntity> findBySchedule_ScheduleId(String scheduleId);
}
