package com.mfcalculator.dto;

import java.time.Instant;

public record PortfolioSummaryResponse(
    Long id,
    String name,
    int holdingCount,
    Instant createdAt
) {}
