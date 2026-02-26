package com.mfcalculator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record InvestmentRequest(
    @NotBlank(message = "fundId is required") String fundId,
    @NotNull(message = "amount is required") @Positive(message = "amount must be greater than 0") Double amount,
    @NotNull(message = "years is required") @PositiveOrZero(message = "years must be 0 or greater") Integer years
) {}
