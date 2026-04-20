package com.atheor.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "operations",
       indexes = {
           @Index(name = "idx_operations_operation_id", columnList = "operation_id"),
           @Index(name = "idx_operations_schedule_fk",  columnList = "schedule_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", referencedColumnName = "schedule_id", nullable = false)
    private ScheduleEntity schedule;

    @Column(name = "operation_id", nullable = false, length = 255)
    private String operationId;

    @Column(name = "operation_name", nullable = false, length = 500)
    private String operationName;

    @Column(name = "operation_type", nullable = false, length = 100)
    private String operationType;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
