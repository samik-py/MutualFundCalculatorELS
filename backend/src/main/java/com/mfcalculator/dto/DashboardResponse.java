package com.mfcalculator.dto;

public record DashboardResponse(
    double totalCostBasis,
    double totalCurrentValue,
    double totalGainLoss,
    double totalReturnPct
) {}
