package com.example.jobflow.controller;

import com.example.jobflow.dto.JobResponse;
import com.example.jobflow.dto.JobSubmitRequest;
import com.example.jobflow.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.jobflow.model.JobEvent;
import com.example.jobflow.repository.JobEventRepository;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JobEventRepository jobEventRepository; // NEW — was missing

    @PostMapping
    public ResponseEntity<JobResponse> submitJob(@RequestBody JobSubmitRequest request) {
        JobResponse response = jobService.submitJob(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobStatus(@PathVariable Long id) {
        JobResponse response = jobService.getJobStatus(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<JobEvent>> getJobTimeline(@PathVariable Long id) {
        return ResponseEntity.ok(jobEventRepository.findByJobIdOrderByCreatedAtAsc(id));
    }
}