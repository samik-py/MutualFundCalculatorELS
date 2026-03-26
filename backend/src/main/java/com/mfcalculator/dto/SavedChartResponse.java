package com.mfcalculator.dto;

import java.time.Instant;

public record SavedChartResponse(
    Long id,
    String title,
    String fundIds,
    int timeHorizon,
    double amount,
    Instant createdAt
) {}
