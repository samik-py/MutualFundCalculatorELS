package com.mfcalculator.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CompareRequest(
    @NotEmpty(message = "at least one fundId is required")
    @Size(max = 10, message = "compare at most 10 funds at once")
    List<String> fundIds,

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    Double amount,

    @NotNull(message = "years is required")
    @PositiveOrZero(message = "years must be 0 or greater")
    Integer years
) {}
