package com.example.jobflow.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "failed_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FailedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_job_id", nullable = false)
    private Long originalJobId;

    @Column(nullable = false)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "failed_at", updatable = false)
    private LocalDateTime failedAt;

    @PrePersist
    protected void onCreate() {
        this.failedAt = LocalDateTime.now();
    }
}