package com.mfcalculator.error;

import java.util.List;

public record ApiError(String error, String message, List<ApiErrorDetail> details) {}
