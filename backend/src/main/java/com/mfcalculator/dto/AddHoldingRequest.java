package com.mfcalculator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record AddHoldingRequest(
    @NotBlank(message = "fundId is required") String fundId,
    @NotNull(message = "shares is required") @Positive(message = "shares must be positive") Double shares,
    @NotNull(message = "purchasePrice is required") @Positive(message = "purchasePrice must be positive") Double purchasePrice,
    @NotNull(message = "purchaseDate is required") LocalDate purchaseDate
) {}
