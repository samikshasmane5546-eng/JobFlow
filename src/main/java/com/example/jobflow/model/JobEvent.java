package com.example.jobflow.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "event_type", nullable = false)
    private String eventType; // QUEUED, PICKED_UP, COMPLETED, FAILED, RETRY_SCHEDULED, DEAD_LETTERED, STUCK_RECOVERED

    @Column(length = 500)
    private String detail;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}