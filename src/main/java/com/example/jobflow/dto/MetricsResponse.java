package com.example.jobflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MetricsResponse {
    private double avgProcessingTimeSeconds;
    private double throughputPerMinute;
    private double failureRatePercent;
}