package com.mfcalculator.dto;

import java.util.List;

public record CryptoTaxOptimizerResponse(
    double estimatedTaxSavings,
    double fifoTaxableGain,
    double hifoTaxableGain,
    String recommendedMethod,
    String summary,
    List<CryptoLotRecommendationResponse> recommendations
) {}
