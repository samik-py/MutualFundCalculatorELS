package com.mfcalculator.dto;

import java.time.LocalDate;

public record HoldingDetailResponse(
    Long id,
    String fundId,
    double shares,
    double purchasePrice,
    LocalDate purchaseDate
) {}
