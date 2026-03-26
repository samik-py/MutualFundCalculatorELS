package com.mfcalculator.dto;

public record HoldingPerformanceItem(
    String fundId,
    String fundName,
    String ticker,
    double shares,
    double costBasis,
    double currentValue,
    double gainLoss,
    double returnPct
) {}
