package com.mfcalculator.service;

import java.util.Locale;

public enum PortfolioRiskProfile {
  VERY_AGGRESSIVE("Very Aggressive"),
  AGGRESSIVE("Aggressive"),
  SLIGHTLY_AGGRESSIVE("Slightly Aggressive"),
  MODERATE("Moderate"),
  SLIGHTLY_CONSERVATIVE("Slightly Conservative"),
  CONSERVATIVE("Conservative"),
  VERY_CONSERVATIVE("Very Conservative");

  private final String displayName;

  PortfolioRiskProfile(String displayName) {
    this.displayName = displayName;
  }

  public String displayName() {
    return displayName;
  }

  public static PortfolioRiskProfile fromModelValue(String value) {
    if (value == null || value.isBlank()) {
      return MODERATE;
    }
    String normalized = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    return switch (normalized) {
      case "VERY_AGGRESSIVE" -> VERY_AGGRESSIVE;
      case "AGGRESSIVE" -> AGGRESSIVE;
      case "SLIGHTLY_AGGRESSIVE" -> SLIGHTLY_AGGRESSIVE;
      case "MODERATE" -> MODERATE;
      case "SLIGHTLY_CONSERVATIVE" -> SLIGHTLY_CONSERVATIVE;
      case "CONSERVATIVE" -> CONSERVATIVE;
      case "VERY_CONSERVATIVE" -> VERY_CONSERVATIVE;
      default -> MODERATE;
    };
  }
}
