package com.mfcalculator.dto;

import java.util.List;

public record PortfolioResponse(List<PortfolioAllocation> allocation, String summary, String expectedReturn) {}
