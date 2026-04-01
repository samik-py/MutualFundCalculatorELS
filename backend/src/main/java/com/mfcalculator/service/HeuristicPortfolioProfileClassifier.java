package com.mfcalculator.service;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class HeuristicPortfolioProfileClassifier implements PortfolioProfileClassifier {
  @Override
  public PortfolioRiskProfile classify(String prompt) {
    if (prompt == null || prompt.isBlank()) {
      return PortfolioRiskProfile.MODERATE;
    }

    String lower = prompt.toLowerCase(Locale.ROOT);

    if (containsAny(lower, "very aggressive", "maximum growth", "highest risk", "speculative")) {
      return PortfolioRiskProfile.VERY_AGGRESSIVE;
    }
    if (containsAny(lower, "slightly aggressive", "moderately aggressive")) {
      return PortfolioRiskProfile.SLIGHTLY_AGGRESSIVE;
    }
    if (containsAny(lower, "very conservative", "minimal risk", "principal protection", "capital preservation only")) {
      return PortfolioRiskProfile.VERY_CONSERVATIVE;
    }
    if (containsAny(lower, "slightly conservative", "modestly conservative", "cautious growth")) {
      return PortfolioRiskProfile.SLIGHTLY_CONSERVATIVE;
    }
    if (containsAny(lower, "conservative", "low risk", "safe", "retire", "preserv")) {
      return PortfolioRiskProfile.CONSERVATIVE;
    }
    if (containsAny(lower, "aggressive", "high risk", "growth", "long term", "young")) {
      return PortfolioRiskProfile.AGGRESSIVE;
    }

    return PortfolioRiskProfile.MODERATE;
  }

  private boolean containsAny(String text, String... needles) {
    for (String needle : needles) {
      if (text.contains(needle)) {
        return true;
      }
    }
    return false;
  }
}
