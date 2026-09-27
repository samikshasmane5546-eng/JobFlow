package com.example.jobflow.dto;

import com.example.jobflow.model.Priority;
import lombok.Data;

@Data
public class JobSubmitRequest {
    private String type;
    private String payload;
    private Priority priority;
    private String idempotencyKey; // optional — client supplies to prevent duplicate submits
}