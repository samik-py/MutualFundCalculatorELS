package com.mfcalculator.dto;

import java.time.LocalDateTime;

public record UserProfileResponse(
    Long id,
    String email,
    String displayName,
    LocalDateTime createdAt
) {}
