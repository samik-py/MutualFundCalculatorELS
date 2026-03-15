package com.mfcalculator.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

public record PortfolioCompareRequest(
    @NotEmpty(message = "portfolioA must have at least one holding") List<PortfolioHolding> portfolioA,
    @NotEmpty(message = "portfolioB must have at least one holding") List<PortfolioHolding> portfolioB,

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    Double amount,

    @NotNull(message = "years is required")
    @PositiveOrZero(message = "years must be 0 or greater")
    Integer years,

    String nameA,
    String nameB
) {}
