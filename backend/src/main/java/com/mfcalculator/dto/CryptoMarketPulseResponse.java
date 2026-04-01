package com.mfcalculator.dto;

import java.util.List;

public record CryptoMarketPulseResponse(
    String primaryPair,
    double midPrice,
    double spreadBps,
    double bidDepthUsd,
    double askDepthUsd,
    double imbalancePct,
    List<String> highlights
) {}
