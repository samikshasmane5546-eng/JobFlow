package com.example.jobflow.controller;

import com.example.jobflow.model.FailedJob;
import com.example.jobflow.model.Job;
import com.example.jobflow.model.JobStatus;
import com.example.jobflow.repository.FailedJobRepository;
import com.example.jobflow.repository.JobRepository;
import com.example.jobflow.service.JobEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/dead-letter")
@RequiredArgsConstructor
public class DeadLetterController {

    private final FailedJobRepository failedJobRepository;
    private final JobRepository jobRepository;
    private final JobEventService jobEventService;

    @GetMapping
    public List<FailedJob> listDeadLetteredJobs() {
        return failedJobRepository.findAll();
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Job> retryDeadLetteredJob(@PathVariable Long id) {

        FailedJob failedJob = failedJobRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Failed job not found: " + id));

        Job job = jobRepository.findById(failedJob.getOriginalJobId())
                .orElseThrow(() -> new NoSuchElementException("Original job not found: " + failedJob.getOriginalJobId()));

        job.setStatus(JobStatus.PENDING);
        job.setRetryCount(0); // reset — operator is giving it a fresh full set of attempts
        job.setNextRetryAt(LocalDateTime.now());
        job.setLockedBy(null);
        job.setLockedAt(null);

        Job saved = jobRepository.save(job);

        jobEventService.log(saved.getId(), "MANUAL_RETRY",
                "Manually requeued from dead-letter queue by operator (was failed job id " + failedJob.getId() + ")");

        return ResponseEntity.ok(saved);
    }
}