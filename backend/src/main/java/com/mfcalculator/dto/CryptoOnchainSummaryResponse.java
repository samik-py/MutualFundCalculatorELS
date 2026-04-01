package com.mfcalculator.dto;

import java.util.List;

public record CryptoOnchainSummaryResponse(
    boolean baseActivityDetected,
    int walletCount,
    int transactionCount30d,
    double gasSpentUsd30d,
    double bridgeVolumeUsd30d,
    List<String> highlights
) {}
