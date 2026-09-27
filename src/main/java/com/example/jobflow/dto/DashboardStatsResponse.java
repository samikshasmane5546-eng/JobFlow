package com.example.jobflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardStatsResponse {
    private long pending;
    private long processing;
    private long completed;
    private long failed;
    private long deadLettered;
    private long total;
}