package com.atheor.backend.repository;

import com.atheor.backend.entity.ScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleRepository extends JpaRepository<ScheduleEntity, UUID> {

    Optional<ScheduleEntity> findByScheduleId(String scheduleId);

    boolean existsByScheduleId(String scheduleId);
}
