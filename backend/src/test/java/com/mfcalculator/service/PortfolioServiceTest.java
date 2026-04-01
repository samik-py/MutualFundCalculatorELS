package com.mfcalculator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mfcalculator.dto.PortfolioAllocation;
import com.mfcalculator.dto.PortfolioResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PortfolioServiceTest {

  private PortfolioService service;

  @BeforeEach
  void setUp() {
    service = new PortfolioService(new HeuristicPortfolioProfileClassifier());
  }

  @Test
  void veryAggressiveProfileReturnsHighestGrowthPortfolio() {
    PortfolioResponse response = service.generate("I want a very aggressive maximum growth portfolio");
    assertNotNull(response);
    PortfolioAllocation top = response.allocation().get(0);
    assertTrue(top.name().contains("FDGRX") || top.name().contains("Fidelity"),
        "Top holding for very aggressive should be a growth fund");
    assertEquals("Very Aggressive", response.riskLevel());
  }

  @Test
  void aggressiveKeywordMapsToAggressiveProfile() {
    PortfolioResponse aggressive = service.generate("aggressive long term");
    assertEquals("Aggressive", aggressive.riskLevel());
    assertEquals("12.1%", aggressive.expectedReturn());
  }

  @Test
  void slightlyAggressiveKeywordMapsToIntermediateGrowthProfile() {
    PortfolioResponse response = service.generate("I want something slightly aggressive");
    assertEquals("Slightly Aggressive", response.riskLevel());
    assertEquals("10.7%", response.expectedReturn());
  }

  @Test
  void conservativeKeywordReturnsCapitalPreservationPortfolio() {
    PortfolioResponse response = service.generate("I want something conservative and low risk");
    assertNotNull(response);
    PortfolioAllocation top = response.allocation().get(0);
    assertTrue(top.name().contains("PTTRX") || top.name().contains("PIMCO"),
        "Top holding for conservative should be a fixed-income fund");
    assertEquals("Conservative", response.riskLevel());
  }

  @Test
  void veryConservativeKeywordMapsToMostDefensiveProfile() {
    PortfolioResponse response = service.generate("very conservative principal protection");
    assertEquals("Very Conservative", response.riskLevel());
    assertEquals("5.4%", response.expectedReturn());
  }

  @Test
  void slightlyConservativeKeywordMapsToCautiousProfile() {
    PortfolioResponse response = service.generate("slightly conservative and cautious growth");
    assertEquals("Slightly Conservative", response.riskLevel());
    assertEquals("8.0%", response.expectedReturn());
  }

  @Test
  void moderatePromptReturnsBalancedPortfolio() {
    PortfolioResponse response = service.generate("I want a balanced allocation");
    assertNotNull(response);
    PortfolioAllocation top = response.allocation().get(0);
    assertEquals("Moderate", response.riskLevel());
    assertTrue(top.name().contains("VFIAX") || top.name().contains("Vanguard"),
        "Top holding for balanced should be Vanguard 500");
  }

  @Test
  void nullPromptReturnsModeratePortfolio() {
    PortfolioResponse balanced = service.generate("balanced");
    PortfolioResponse nullProm = service.generate(null);
    assertEquals(balanced.expectedReturn(), nullProm.expectedReturn());
  }

  @Test
  void emptyPromptReturnsModeratePortfolio() {
    PortfolioResponse response = service.generate("");
    assertNotNull(response);
    assertEquals("9.3%", response.expectedReturn());
  }

  @Test
  void aggressivePortfolioHasFourAllocations() {
    assertEquals(4, service.generate("aggressive").allocation().size());
  }

  @Test
  void conservativePortfolioHasFourAllocations() {
    assertEquals(4, service.generate("conservative safe").allocation().size());
  }

  @Test
  void moderatePortfolioHasFourAllocations() {
    assertEquals(4, service.generate("moderate").allocation().size());
  }

  @Test
  void aggressiveAllocationsSumTo100Percent() {
    int total = service.generate("aggressive growth")
        .allocation().stream()
        .mapToInt(PortfolioAllocation::pct)
        .sum();
    assertEquals(100, total);
  }

  @Test
  void conservativeAllocationsSumTo100Percent() {
    int total = service.generate("conservative low risk")
        .allocation().stream()
        .mapToInt(PortfolioAllocation::pct)
        .sum();
    assertEquals(100, total);
  }

  @Test
  void moderateAllocationsSumTo100Percent() {
    int total = service.generate("balanced income")
        .allocation().stream()
        .mapToInt(PortfolioAllocation::pct)
        .sum();
    assertEquals(100, total);
  }

  @Test
  void allPortfolioResponsesHaveNonBlankSummary() {
    for (String prompt : new String[]{"very aggressive", "aggressive", "balanced", "slightly conservative", ""}) {
      PortfolioResponse r = service.generate(prompt);
      assertFalse(r.summary().isBlank(), "Summary should not be blank for prompt: " + prompt);
    }
  }

  @Test
  void allPortfolioResponsesHaveNonBlankExpectedReturn() {
    for (String prompt : new String[]{"very aggressive", "aggressive", "conservative", "balanced", "very conservative"}) {
      PortfolioResponse r = service.generate(prompt);
      assertFalse(r.expectedReturn().isBlank(),
          "expectedReturn should not be blank for prompt: " + prompt);
    }
  }

  @Test
  void allPortfolioAllocationsSumTo100Percent() {
    for (String prompt : new String[]{
        "very aggressive growth",
        "aggressive growth",
        "slightly aggressive growth",
        "moderate balanced",
        "slightly conservative income",
        "conservative retire",
        "very conservative safe"
    }) {
      int total = service.generate(prompt)
          .allocation().stream()
          .mapToInt(PortfolioAllocation::pct)
          .sum();
      assertEquals(100, total);
    }
  }

  @Test
  void moderateExpectedReturnIs9Point3Percent() {
    assertEquals("9.3%", service.generate("some random prompt").expectedReturn());
  }

  @Test
  void portfolioForNullProfileFallsBackToModerate() {
    PortfolioResponse response = service.portfolioFor(null);
    assertEquals("Moderate", response.riskLevel());
    assertEquals("9.3%", response.expectedReturn());
  }
}
