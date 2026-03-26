package com.mfcalculator.dto;

import java.util.List;

public record PortfolioPerformanceResponse(
    List<HoldingPerformanceItem> holdings,
    double totalCostBasis,
    double totalCurrentValue,
    double totalGainLoss,
    double totalReturnPct
) {}
