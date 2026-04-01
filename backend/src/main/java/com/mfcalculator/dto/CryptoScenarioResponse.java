package com.mfcalculator.dto;

public record CryptoScenarioResponse(
    String name,
    double oneYearMedian,
    double threeYearMedian,
    double fiveYearMedian,
    double downside95,
    double upside95
) {}
