package com.mfcalculator.dto;

import java.util.List;

public record PortfolioCompareResponse(
    List<YearlyDataPoint> portfolioA,
    List<YearlyDataPoint> portfolioB,
    String nameA,
    String nameB,
    double annualReturnA,
    double annualReturnB
) {}
