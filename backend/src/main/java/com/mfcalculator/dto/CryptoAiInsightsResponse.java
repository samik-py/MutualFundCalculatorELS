package com.mfcalculator.dto;

import java.util.List;

public record CryptoAiInsightsResponse(
    String headline,
    String summary,
    List<String> bullets,
    String outlook
) {}
