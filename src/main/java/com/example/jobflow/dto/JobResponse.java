package com.example.jobflow.dto;

import com.example.jobflow.model.JobStatus;
import com.example.jobflow.model.Priority;
import lombok.Data;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class JobResponse {
    private Long id;
    private String type;
    private JobStatus status;
    private Priority priority;
    private int retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}