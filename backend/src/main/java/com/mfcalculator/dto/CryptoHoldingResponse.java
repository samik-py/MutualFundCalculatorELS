package com.mfcalculator.dto;

public record CryptoHoldingResponse(
    String asset,
    String name,
    double quantity,
    double costBasis,
    double currentPrice,
    double currentValue,
    double unrealizedPnL,
    double unrealizedPnLPct
) {}
