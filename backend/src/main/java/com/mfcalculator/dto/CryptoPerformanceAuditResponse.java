package com.mfcalculator.dto;

import java.util.List;

public record CryptoPerformanceAuditResponse(
    double realizedPnL,
    double holdVsSellDelta,
    double missedPeakUpside,
    double auditScore,
    String verdict,
    List<String> highlights
) {}
