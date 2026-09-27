package com.example.jobflow.service;

import com.example.jobflow.model.FailedJob;
import com.example.jobflow.model.Job;
import com.example.jobflow.model.JobStatus;
import com.example.jobflow.repository.FailedJobRepository;
import com.example.jobflow.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * Each method here is its own short, independent transaction that commits
 * immediately on return. This is deliberate: claiming a job must be visible
 * to other parts of the system (like stuck-job recovery) BEFORE the actual
 * job processing (which can take a long time) even starts.
 */
@Service
@RequiredArgsConstructor
public class JobTransactionService {

    private final JobRepository jobRepository;
    private final FailedJobRepository failedJobRepository;
    private final JobEventService jobEventService;
    private final Random random = new Random();

    @Transactional
    public List<Job> claimJobs(int batchSize, String workerId) {
        List<Job> jobs = jobRepository.findAndLockNextJobs(LocalDateTime.now(), batchSize);

        for (Job job : jobs) {
            job.setStatus(JobStatus.PROCESSING);
            job.setLockedBy(workerId);
            job.setLockedAt(LocalDateTime.now());
            jobRepository.save(job);
            jobEventService.log(job.getId(), "PICKED_UP", "Picked up by " + workerId);
        }

        return jobs; // committed the moment this method returns
    }

    @Transactional
    public void markCompleted(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus(JobStatus.COMPLETED);
        job.setLockedBy(null);
        job.setLockedAt(null);
        jobRepository.save(job);
        jobEventService.log(jobId, "COMPLETED", "Job completed successfully");
    }

    @Transactional
    public void markDeadLettered(Long jobId, String errorMessage) {
        Job job = jobRepository.findById(jobId).orElseThrow();
        job.setLockedBy(null);
        job.setLockedAt(null);
        job.setStatus(JobStatus.DEAD_LETTERED);
        jobRepository.save(job);

        FailedJob failedJob = new FailedJob();
        failedJob.setOriginalJobId(job.getId());
        failedJob.setType(job.getType());
        failedJob.setPayload(job.getPayload());
        failedJob.setFailureReason(errorMessage);
        failedJobRepository.save(failedJob);

        jobEventService.log(jobId, "DEAD_LETTERED", errorMessage);
    }

    @Transactional
    public void markFailedAndMaybeRetry(Long jobId, String errorMessage) {
        Job job = jobRepository.findById(jobId).orElseThrow();

        int newRetryCount = job.getRetryCount() + 1;
        job.setRetryCount(newRetryCount);
        job.setLockedBy(null);
        job.setLockedAt(null);

        if (newRetryCount >= job.getMaxRetries()) {
            markDeadLetteredInline(job, errorMessage);
        } else {
            long backoffSeconds = (long) Math.pow(2, newRetryCount);
            long jitterMillis = random.nextInt(1000);

            job.setStatus(JobStatus.PENDING);
            job.setNextRetryAt(
                    LocalDateTime.now().plusSeconds(backoffSeconds).plusNanos(jitterMillis * 1_000_000)
            );
            jobRepository.save(job);

            jobEventService.log(jobId, "RETRY_SCHEDULED",
                    "Attempt " + newRetryCount + " failed (" + errorMessage + "), retry in " + backoffSeconds + "s");
        }
    }

    private void markDeadLetteredInline(Job job, String errorMessage) {
        job.setStatus(JobStatus.DEAD_LETTERED);
        jobRepository.save(job);

        FailedJob failedJob = new FailedJob();
        failedJob.setOriginalJobId(job.getId());
        failedJob.setType(job.getType());
        failedJob.setPayload(job.getPayload());
        failedJob.setFailureReason(errorMessage);
        failedJobRepository.save(failedJob);

        jobEventService.log(job.getId(), "DEAD_LETTERED", errorMessage);
    }
}