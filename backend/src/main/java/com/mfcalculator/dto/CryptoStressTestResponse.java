package com.mfcalculator.dto;

import java.util.List;

public record CryptoStressTestResponse(
    double currentPortfolioValue,
    List<CryptoScenarioResponse> scenarios,
    List<CryptoFanPointResponse> fanChart
) {}
