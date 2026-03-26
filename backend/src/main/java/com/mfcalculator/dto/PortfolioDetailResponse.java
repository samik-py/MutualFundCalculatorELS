package com.mfcalculator.dto;

import java.time.Instant;
import java.util.List;

public record PortfolioDetailResponse(
    Long id,
    String name,
    List<HoldingDetailResponse> holdings,
    Instant createdAt
) {}
