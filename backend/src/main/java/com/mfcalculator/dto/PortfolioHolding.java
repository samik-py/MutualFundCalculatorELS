package com.mfcalculator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PortfolioHolding(
    @NotBlank(message = "fundId is required") String fundId,
    @NotNull(message = "weight is required") @Positive(message = "weight must be positive") Double weight
) {}
