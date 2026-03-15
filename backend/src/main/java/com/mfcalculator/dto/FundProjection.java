package com.mfcalculator.dto;

import java.util.List;

public record FundProjection(
    String fundId,
    String name,
    double annualReturn,
    List<YearlyDataPoint> projection
) {}
