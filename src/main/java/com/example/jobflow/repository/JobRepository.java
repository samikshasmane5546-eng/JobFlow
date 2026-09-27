package com.example.jobflow.repository;

import com.example.jobflow.model.Job;
import com.example.jobflow.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    // Fetch next batch of jobs ready to run, highest priority first,
    // using FOR UPDATE SKIP LOCKED so concurrent workers never grab the same row.
    @Query(value = """
            SELECT * FROM jobs
            WHERE status = 'PENDING'
            AND next_retry_at <= :now
            ORDER BY
                CASE priority
                    WHEN 'HIGH' THEN 1
                    WHEN 'MEDIUM' THEN 2
                    WHEN 'LOW' THEN 3
                END,
                created_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Job> findAndLockNextJobs(@Param("now") LocalDateTime now, @Param("batchSize") int batchSize);

    // Finds jobs stuck in PROCESSING longer than the stale threshold —
    // used by StuckJobRecoveryService to detect crashed/timed-out workers.
    @Query(value = """
            SELECT * FROM jobs
            WHERE status = 'PROCESSING'
            AND locked_at <= :staleThreshold
            """, nativeQuery = true)
    List<Job> findStuckJobs(@Param("staleThreshold") LocalDateTime staleThreshold);

    // NEW: used for idempotency check on submit
    Optional<Job> findByIdempotencyKey(String idempotencyKey);

    List<Job> findByStatus(JobStatus status);

    long countByStatus(JobStatus status);

    @Query(value = """
        SELECT AVG(TIMESTAMPDIFF(SECOND, created_at, updated_at))
        FROM jobs
        WHERE status = 'COMPLETED'
        """, nativeQuery = true)
    Double findAvgProcessingTimeSeconds();

    @Query(value = """
        SELECT COUNT(*) FROM jobs
        WHERE status = 'COMPLETED'
        AND updated_at >= :since
        """, nativeQuery = true)
    long countCompletedSince(@Param("since") java.time.LocalDateTime since);
}