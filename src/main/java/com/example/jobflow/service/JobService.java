package com.example.jobflow.service;

import com.example.jobflow.dto.JobResponse;
import com.example.jobflow.dto.JobSubmitRequest;
import com.example.jobflow.exception.NonRetryableException;
import com.example.jobflow.model.Job;
import com.example.jobflow.model.JobStatus;
import com.example.jobflow.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobEventService jobEventService;
    private final JobTransactionService jobTransactionService; // NEW
    private final Random random = new Random();

    public JobResponse submitJob(JobSubmitRequest request) {

        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<Job> existing = jobRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                System.out.println("Idempotent resubmit detected — returning existing job id " + existing.get().getId());
                return toResponse(existing.get());
            }
        }

        Job job = new Job();

        job.setType(request.getType());
        job.setPayload(request.getPayload());
        job.setPriority(request.getPriority());
        job.setStatus(JobStatus.PENDING);
        job.setRetryCount(0);
        job.setMaxRetries(5);
        job.setIdempotencyKey(request.getIdempotencyKey());

        Job saved = jobRepository.save(job);

        jobEventService.log(saved.getId(), "QUEUED", "Job added to queue with priority " + saved.getPriority());

        return toResponse(saved);
    }

    public JobResponse getJobStatus(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Job not found: " + id)
                );

        return toResponse(job);
    }

    // NOTE: no longer @Transactional at this level on purpose.
    // claimJobs/markCompleted/markDeadLettered/markFailedAndMaybeRetry each
    // run in their OWN short transaction inside JobTransactionService, so
    // that a PROCESSING status is committed and visible to other parts of
    // the system (like StuckJobRecoveryService) BEFORE the potentially
    // long-running executeJob() call even starts.
    public void processNextBatch(int batchSize) {

        String workerId = "worker-" + Thread.currentThread().getId();
        List<Job> jobs = jobTransactionService.claimJobs(batchSize, workerId); // commits immediately

        for (Job job : jobs) {
            try {
                executeJob(job); // free to sleep/take time — no open transaction, no hidden lock held

                jobTransactionService.markCompleted(job.getId());
                System.out.println("[Job " + job.getId() + "] COMPLETED");

            } catch (NonRetryableException e) {

                jobTransactionService.markDeadLettered(job.getId(), e.getMessage());

            } catch (Exception e) {

                jobTransactionService.markFailedAndMaybeRetry(job.getId(), e.getMessage());
            }
        }
    }

    private void executeJob(Job job) throws Exception {
        Thread.sleep(1000);

        String payload = job.getPayload();

        if (payload != null && payload.contains("\"simulateFailure\":\"MALFORMED\"")) {
            throw new NonRetryableException("Malformed payload for job type: " + job.getType());
        }

        if (payload != null && payload.contains("\"simulateFailure\":\"TIMEOUT\"")) {
            Thread.sleep(60000);
        }

        if (payload != null && payload.contains("\"simulateFailure\":\"CRASH\"")) {
            Runtime.getRuntime().halt(1);
        }

        if (payload != null && payload.contains("\"simulateFailure\":\"TRANSIENT\"")) {
            throw new Exception("Simulated transient failure for job type: " + job.getType());
        }

        if (payload != null && payload.contains("\"forceFail\":true")) {
            throw new Exception("Forced failure for testing job type: " + job.getType());
        }

        if (random.nextInt(10) < 3) {
            throw new Exception("Simulated failure processing job type: " + job.getType());
        }
    }

    private JobResponse toResponse(Job job) {

        return new JobResponse(
                job.getId(),
                job.getType(),
                job.getStatus(),
                job.getPriority(),
                job.getRetryCount(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}