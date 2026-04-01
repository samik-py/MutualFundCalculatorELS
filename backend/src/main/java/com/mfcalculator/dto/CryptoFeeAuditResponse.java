package com.mfcalculator.dto;

import java.util.List;

public record CryptoFeeAuditResponse(
    double totalCoinbaseFees,
    double estimatedSpreadCost,
    double advancedTradeEquivalentCost,
    double potentialSavings,
    double effectiveCostRatePct,
    List<String> highlights
) {}
