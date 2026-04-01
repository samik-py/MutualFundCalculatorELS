package com.mfcalculator.dto;

public record CryptoUploadResponse(
    String fileName,
    int holdingsImported,
    int transactionsImported,
    String uploadedAt,
    CryptoSuiteResponse suite
) {}
