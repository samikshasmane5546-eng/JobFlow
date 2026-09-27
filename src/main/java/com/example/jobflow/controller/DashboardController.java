package com.example.jobflow.controller;

import com.example.jobflow.dto.DashboardStatsResponse;
import com.example.jobflow.dto.MetricsResponse;
import com.example.jobflow.model.JobStatus;
import com.example.jobflow.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final JobRepository jobRepository;

    @GetMapping("/stats")
    public DashboardStatsResponse getStats() {

        long pending = jobRepository.countByStatus(JobStatus.PENDING);
        long processing = jobRepository.countByStatus(JobStatus.PROCESSING);
        long completed = jobRepository.countByStatus(JobStatus.COMPLETED);
        long failed = jobRepository.countByStatus(JobStatus.FAILED);
        long deadLettered = jobRepository.countByStatus(JobStatus.DEAD_LETTERED);
        long total = jobRepository.count();

        return new DashboardStatsResponse(pending, processing, completed, failed, deadLettered, total);
    }

    @GetMapping("/metrics")
    public MetricsResponse getMetrics() {

        Double avgTime = jobRepository.findAvgProcessingTimeSeconds();
        double avgProcessingTime = (avgTime != null) ? avgTime : 0.0;

        long completedLastMinute = jobRepository.countCompletedSince(
                java.time.LocalDateTime.now().minusMinutes(1)
        );

        long completed = jobRepository.countByStatus(JobStatus.COMPLETED);
        long deadLettered = jobRepository.countByStatus(JobStatus.DEAD_LETTERED);
        long totalFinished = completed + deadLettered;

        double failureRate = (totalFinished == 0)
                ? 0.0
                : (deadLettered * 100.0) / totalFinished;

        return new MetricsResponse(avgProcessingTime, (double) completedLastMinute, failureRate);
    }
}