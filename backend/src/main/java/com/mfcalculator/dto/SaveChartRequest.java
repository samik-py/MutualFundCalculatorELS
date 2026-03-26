package com.mfcalculator.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SaveChartRequest(
    @NotBlank(message = "title is required") String title,
    @NotBlank(message = "fundIds is required") String fundIds,
    @NotNull(message = "timeHorizon is required") @Min(1) @Max(30) Integer timeHorizon,
    @NotNull(message = "amount is required") @DecimalMin("0.01") Double amount
) {}
