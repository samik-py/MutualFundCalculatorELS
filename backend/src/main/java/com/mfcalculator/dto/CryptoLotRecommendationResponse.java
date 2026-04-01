package com.mfcalculator.dto;

public record CryptoLotRecommendationResponse(
    String asset,
    String acquiredAt,
    double quantity,
    double unitCost,
    double currentPrice,
    double estimatedGain,
    double estimatedTaxImpact,
    String rationale
) {}
