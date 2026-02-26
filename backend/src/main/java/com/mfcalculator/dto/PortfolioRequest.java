package com.mfcalculator.dto;

import jakarta.validation.constraints.NotBlank;

public record PortfolioRequest(
    @NotBlank(message = "prompt is required") String prompt
) {}
