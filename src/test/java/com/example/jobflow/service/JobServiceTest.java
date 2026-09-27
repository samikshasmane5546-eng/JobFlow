package com.example.jobflow.service;

import com.example.jobflow.dto.JobSubmitRequest;
import com.example.jobflow.dto.JobResponse;
import com.example.jobflow.model.JobStatus;
import com.example.jobflow.model.Priority;
import com.example.jobflow.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JobServiceTest {

    @Autowired
    private JobService jobService;

    @Autowired
    private JobRepository jobRepository;

    @Test
    void submittingAJob_createsItAsPendingWithDefaults() {
        JobSubmitRequest request = new JobSubmitRequest();
        request.setType("email");
        request.setPayload("{\"to\":\"test@example.com\"}");
        request.setPriority(Priority.MEDIUM);

        JobResponse response = jobService.submitJob(request);

        assertNotNull(response.getId());
        assertEquals(JobStatus.PENDING, response.getStatus());
        assertEquals(0, response.getRetryCount());
    }

    @Test
    void submittingWithSameIdempotencyKeyTwice_returnsSameJob_notDuplicate() {
        JobSubmitRequest request = new JobSubmitRequest();
        request.setType("pdf-export");
        request.setPayload("{\"reportId\":\"rpt_1\"}");
        request.setPriority(Priority.HIGH);
        request.setIdempotencyKey("test-key-001");

        JobResponse first = jobService.submitJob(request);
        JobResponse second = jobService.submitJob(request);

        assertEquals(first.getId(), second.getId());
    }

    @Test
    void malformedPayload_goesStraightToDeadLetter_noRetries() throws InterruptedException {
        JobSubmitRequest request = new JobSubmitRequest();
        request.setType("image-resize");
        request.setPayload("{\"simulateFailure\":\"MALFORMED\"}");
        request.setPriority(Priority.HIGH);

        JobResponse submitted = jobService.submitJob(request);

        // process it directly instead of waiting for the worker pool's poll cycle
        jobService.processNextBatch(10);

        JobResponse result = jobService.getJobStatus(submitted.getId());

        assertEquals(JobStatus.DEAD_LETTERED, result.getStatus());
        assertEquals(0, result.getRetryCount()); // key assertion: no retries were wasted
    }

    @Test
    void getJobStatus_throwsWhenJobDoesNotExist() {
        assertThrows(java.util.NoSuchElementException.class, () -> {
            jobService.getJobStatus(999999L);
        });
    }

}