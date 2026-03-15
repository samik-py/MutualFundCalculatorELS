package com.mfcalculator.dto;

public record MarketIndicatorsResponse(
    double riskFreeRate,
    double marketReturn5y,
    String dataAsOf,
    String rfSource,
    String rmSource
) {}
