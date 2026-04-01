package com.mfcalculator.dto;

public record CryptoFanPointResponse(
    int year,
    double p5,
    double p25,
    double median,
    double p75,
    double p95
) {}
