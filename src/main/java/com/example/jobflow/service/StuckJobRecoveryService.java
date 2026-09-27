package com.example.jobflow.service;

import com.example.jobflow.model.Job;
import com.example.jobflow.model.JobStatus;
import com.example.jobflow.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StuckJobRecoveryService {

    private final JobRepository jobRepository;
    private final JobEventService jobEventService; // NEW

    private static final long STUCK_THRESHOLD_SECONDS = 30;

    @Scheduled(fixedRate = 10000)
    @Transactional
    public void recoverStuckJobs() {

        LocalDateTime staleThreshold = LocalDateTime.now().minusSeconds(STUCK_THRESHOLD_SECONDS);

        List<Job> stuckJobs = jobRepository.findStuckJobs(staleThreshold);

        for (Job job : stuckJobs) {
            System.out.println(
                    "[Job " + job.getId() + "] detected STUCK (locked by "
                            + job.getLockedBy() + " since " + job.getLockedAt()
                            + ") — requeuing"
            );

            jobEventService.log(job.getId(), "STUCK_RECOVERED", // NEW
                    "Was locked by " + job.getLockedBy() + " since " + job.getLockedAt() + " — requeued");

            job.setStatus(JobStatus.PENDING);
            job.setLockedBy(null);
            job.setLockedAt(null);
            job.setRetryCount(job.getRetryCount() + 1);

            jobRepository.save(job);
        }
    }
}