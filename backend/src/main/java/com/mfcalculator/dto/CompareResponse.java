package com.mfcalculator.dto;

import java.util.List;

public record CompareResponse(List<FundProjection> funds, double initialAmount) {}
