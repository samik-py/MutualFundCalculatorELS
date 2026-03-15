package com.mfcalculator.dto;

import java.util.List;

public record MonteCarloResponse(
    List<PercentileSeries> series,
    double baseReturn,
    double estimatedVolatility,
    double estimatedBeta
) {}
