package com.atheor.backend.service;

import com.atheor.backend.entity.ScheduleEntity;
import com.atheor.backend.repository.ScheduleRepository;
import com.atheor.common.model.EventStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackendService {

    private final ScheduleRepository scheduleRepository;

    @Transactional
    public ScheduleEntity saveSchedule(ScheduleEntity entity) {
        log.info("Persisting schedule [scheduleId={}]", entity.getScheduleId());
        return scheduleRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public Optional<ScheduleEntity> findByScheduleId(String scheduleId) {
        return scheduleRepository.findByScheduleId(scheduleId);
    }

    @Transactional
    public void updateStatus(String scheduleId, EventStatus status) {
        scheduleRepository.findByScheduleId(scheduleId).ifPresent(entity -> {
            log.info("Updating schedule [scheduleId={}] status to {}", scheduleId, status);
            entity.setStatus(status);
            scheduleRepository.save(entity);
        });
    }
}
