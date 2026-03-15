package com.mfcalculator.dto;

import java.util.List;

public record PercentileSeries(String label, int percentile, List<YearlyDataPoint> data) {}
