package com.mfcalculator.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePortfolioRequest(
    @NotBlank(message = "name is required") String name
) {}
