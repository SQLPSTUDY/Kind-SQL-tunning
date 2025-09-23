package org.example.jpapaging.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceResult {
    private String method;
    private long executionTimeMs;
    private int resultCount;
    private String additionalInfo;
}