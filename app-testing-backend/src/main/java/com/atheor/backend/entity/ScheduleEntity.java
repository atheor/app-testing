package com.atheor.backend.entity;

import com.atheor.common.model.EventStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "schedules",
       indexes = {
           @Index(name = "idx_schedules_schedule_id",  columnList = "schedule_id"),
           @Index(name = "idx_schedules_correlation_id", columnList = "correlation_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "schedule_id", unique = true, nullable = false, length = 255)
    private String scheduleId;

    @Column(name = "schedule_name", nullable = false, length = 500)
    private String scheduleName;

    @Column(name = "start_date", nullable = false, length = 20)
    private String startDate;

    @Column(name = "end_date", nullable = false, length = 20)
    private String endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventStatus status;

    @Column(name = "correlation_id", length = 255)
    private String correlationId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "schedule",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<OperationEntity> operations = new ArrayList<>();

    public void addOperation(OperationEntity operation) {
        operations.add(operation);
        operation.setSchedule(this);
    }
}
