package com.mfcalculator.dto;

import java.util.List;

public record CryptoSuiteResponse(
    boolean connected,
    boolean demoMode,
    String provider,
    String connectionLabel,
    String connectionHint,
    String connectUrl,
    String sourceLabel,
    String lastUpdated,
    CryptoAiInsightsResponse aiInsights,
    List<CryptoHoldingResponse> holdings,
    CryptoTaxOptimizerResponse taxOptimizer,
    CryptoPerformanceAuditResponse performanceAudit,
    CryptoFeeAuditResponse feeAudit,
    CryptoStressTestResponse stressTest,
    CryptoMarketPulseResponse marketPulse,
    CryptoOnchainSummaryResponse onchainSummary
) {}
