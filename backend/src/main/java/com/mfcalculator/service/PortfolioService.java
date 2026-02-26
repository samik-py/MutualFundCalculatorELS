package com.mfcalculator.service;

import com.mfcalculator.dto.PortfolioAllocation;
import com.mfcalculator.dto.PortfolioResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PortfolioService {
  public PortfolioResponse generate(String prompt) {
    String profile = classify(prompt);
    return switch (profile) {
      case "aggressive" -> new PortfolioResponse(
          List.of(
              new PortfolioAllocation("Fidelity Growth Company (FDGRX)", 40),
              new PortfolioAllocation("T. Rowe Price Blue Chip (TRBCX)", 30),
              new PortfolioAllocation("Vanguard 500 Index (VFIAX)", 20),
              new PortfolioAllocation("PIMCO Total Return (PTTRX)", 10)
          ),
          "High-growth portfolio optimized for long-term capital appreciation. Weighted heavily toward equity growth funds with minimal fixed-income exposure.",
          "12.4%"
      );
      case "conservative" -> new PortfolioResponse(
          List.of(
              new PortfolioAllocation("PIMCO Total Return (PTTRX)", 45),
              new PortfolioAllocation("Vanguard 500 Index (VFIAX)", 30),
              new PortfolioAllocation("Schwab Total Market (SWTSX)", 15),
              new PortfolioAllocation("Fidelity Growth Company (FDGRX)", 10)
          ),
          "Capital-preservation portfolio prioritizing income stability with modest equity exposure.",
          "6.1%"
      );
      default -> new PortfolioResponse(
          List.of(
              new PortfolioAllocation("Vanguard 500 Index (VFIAX)", 40),
              new PortfolioAllocation("Schwab Total Market (SWTSX)", 25),
              new PortfolioAllocation("T. Rowe Price Blue Chip (TRBCX)", 20),
              new PortfolioAllocation("PIMCO Total Return (PTTRX)", 15)
          ),
          "Balanced portfolio designed for steady growth with moderate volatility.",
          "9.8%"
      );
    };
  }

  private String classify(String prompt) {
    if (prompt == null) {
      return "moderate";
    }
    String lower = prompt.toLowerCase();
    if (lower.contains("aggressive") || lower.contains("high risk") || lower.contains("growth")
        || lower.contains("long term") || lower.contains("young")) {
      return "aggressive";
    }
    if (lower.contains("conservative") || lower.contains("low risk") || lower.contains("safe")
        || lower.contains("retire") || lower.contains("preserv")) {
      return "conservative";
    }
    return "moderate";
  }
}
