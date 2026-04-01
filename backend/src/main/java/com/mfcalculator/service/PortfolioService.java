package com.mfcalculator.service;

import com.mfcalculator.dto.PortfolioAllocation;
import com.mfcalculator.dto.PortfolioResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PortfolioService {
  private final PortfolioProfileClassifier portfolioProfileClassifier;

  public PortfolioService(PortfolioProfileClassifier portfolioProfileClassifier) {
    this.portfolioProfileClassifier = portfolioProfileClassifier;
  }

  public PortfolioResponse generate(String prompt) {
    PortfolioRiskProfile profile = portfolioProfileClassifier.classify(prompt);
    return portfolioFor(profile);
  }

  PortfolioResponse portfolioFor(PortfolioRiskProfile profile) {
    PortfolioRiskProfile resolved = profile == null ? PortfolioRiskProfile.MODERATE : profile;
    return switch (resolved) {
      case VERY_AGGRESSIVE -> new PortfolioResponse(
          List.of(
              new PortfolioAllocation("Fidelity Growth Company (FDGRX)", 35),
              new PortfolioAllocation("T. Rowe Price Blue Chip (TRBCX)", 25),
              new PortfolioAllocation("Vanguard 500 Index (VFIAX)", 20),
              new PortfolioAllocation("Schwab Total Market (SWTSX)", 20)
          ),
          "Very aggressive portfolio focused on maximizing long-horizon capital appreciation, with a strong tilt toward growth-oriented equity funds and no dedicated fixed-income sleeve.",
          "13.6%",
          resolved.displayName()
      );
      case AGGRESSIVE -> new PortfolioResponse(
          List.of(
              new PortfolioAllocation("Fidelity Growth Company (FDGRX)", 30),
              new PortfolioAllocation("T. Rowe Price Blue Chip (TRBCX)", 25),
              new PortfolioAllocation("Vanguard 500 Index (VFIAX)", 25),
              new PortfolioAllocation("Schwab Total Market (SWTSX)", 20)
          ),
          "Aggressive portfolio optimized for long-term growth, emphasizing diversified equity exposure with a bias toward large-cap growth funds.",
          "12.1%",
          resolved.displayName()
      );
      case SLIGHTLY_AGGRESSIVE -> new PortfolioResponse(
          List.of(
              new PortfolioAllocation("Vanguard 500 Index (VFIAX)", 30),
              new PortfolioAllocation("Schwab Total Market (SWTSX)", 25),
              new PortfolioAllocation("T. Rowe Price Blue Chip (TRBCX)", 25),
              new PortfolioAllocation("PIMCO Total Return (PTTRX)", 20)
          ),
          "Slightly aggressive portfolio balancing equity-led growth with a modest bond allocation to soften drawdowns while preserving upside participation.",
          "10.7%",
          resolved.displayName()
      );
      case MODERATE -> new PortfolioResponse(
          List.of(
              new PortfolioAllocation("Vanguard 500 Index (VFIAX)", 30),
              new PortfolioAllocation("Schwab Total Market (SWTSX)", 25),
              new PortfolioAllocation("T. Rowe Price Blue Chip (TRBCX)", 20),
              new PortfolioAllocation("PIMCO Total Return (PTTRX)", 25)
          ),
          "Moderate portfolio designed for steady compounding through a balanced mix of broad equities, growth exposure, and core fixed income.",
          "9.3%",
          resolved.displayName()
      );
      case SLIGHTLY_CONSERVATIVE -> new PortfolioResponse(
          List.of(
              new PortfolioAllocation("PIMCO Total Return (PTTRX)", 35),
              new PortfolioAllocation("Vanguard 500 Index (VFIAX)", 25),
              new PortfolioAllocation("Schwab Total Market (SWTSX)", 20),
              new PortfolioAllocation("T. Rowe Price Blue Chip (TRBCX)", 20)
          ),
          "Slightly conservative portfolio prioritizing smoother returns and income support while retaining meaningful equity exposure for inflation-beating growth.",
          "8.0%",
          resolved.displayName()
      );
      case CONSERVATIVE -> new PortfolioResponse(
          List.of(
              new PortfolioAllocation("PIMCO Total Return (PTTRX)", 45),
              new PortfolioAllocation("Vanguard 500 Index (VFIAX)", 25),
              new PortfolioAllocation("Schwab Total Market (SWTSX)", 15),
              new PortfolioAllocation("T. Rowe Price Blue Chip (TRBCX)", 15)
          ),
          "Conservative portfolio centered on capital preservation and income stability, with limited equity exposure to support modest appreciation over time.",
          "6.6%",
          resolved.displayName()
      );
      case VERY_CONSERVATIVE -> new PortfolioResponse(
          List.of(
              new PortfolioAllocation("PIMCO Total Return (PTTRX)", 55),
              new PortfolioAllocation("Vanguard 500 Index (VFIAX)", 20),
              new PortfolioAllocation("Schwab Total Market (SWTSX)", 15),
              new PortfolioAllocation("T. Rowe Price Blue Chip (TRBCX)", 10)
          ),
          "Very conservative portfolio emphasizing principal stability and low volatility, with a dominant fixed-income allocation and only limited equity participation.",
          "5.4%",
          resolved.displayName()
      );
    };
  }
}
