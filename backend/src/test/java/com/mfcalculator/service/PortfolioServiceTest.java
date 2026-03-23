package com.mfcalculator.service;

import static org.junit.jupiter.api.Assertions.*;

import com.mfcalculator.dto.PortfolioAllocation;
import com.mfcalculator.dto.PortfolioResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PortfolioServiceTest {

  private PortfolioService service;

  @BeforeEach
  void setUp() {
    service = new PortfolioService();
  }

  @Test
  void aggressiveKeywordReturnsHighGrowthPortfolio() {
    PortfolioResponse response = service.generate("I want aggressive growth");
    assertNotNull(response);
    PortfolioAllocation top = response.allocation().get(0);
    assertTrue(top.name().contains("FDGRX") || top.name().contains("Fidelity"),
        "Top holding for aggressive should be a growth fund");
  }

  @Test
  void highRiskKeywordMapsToAggressive() {
    PortfolioResponse aggressive = service.generate("aggressive long term");
    PortfolioResponse highRisk   = service.generate("high risk portfolio");
    assertEquals(aggressive.expectedReturn(), highRisk.expectedReturn());
    assertEquals(aggressive.allocation().size(), highRisk.allocation().size());
  }

  @Test
  void conservativeKeywordReturnsCapitalPreservationPortfolio() {
    PortfolioResponse response = service.generate("I want something conservative");
    assertNotNull(response);
    PortfolioAllocation top = response.allocation().get(0);
    assertTrue(top.name().contains("PTTRX") || top.name().contains("PIMCO"),
        "Top holding for conservative should be a fixed-income fund");
  }

  @Test
  void retireKeywordMapsToConservative() {
    PortfolioResponse conservative = service.generate("conservative");
    PortfolioResponse retire       = service.generate("planning to retire soon");
    assertEquals(conservative.expectedReturn(), retire.expectedReturn());
  }

  @Test
  void moderatePromptReturnsBalancedPortfolio() {
    PortfolioResponse response = service.generate("I want a balanced allocation");
    assertNotNull(response);
    PortfolioAllocation top = response.allocation().get(0);
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
    assertEquals("9.8%", response.expectedReturn());
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
    for (String prompt : new String[]{"aggressive", "conservative", "balanced", ""}) {
      PortfolioResponse r = service.generate(prompt);
      assertFalse(r.summary().isBlank(), "Summary should not be blank for prompt: " + prompt);
    }
  }

  @Test
  void allPortfolioResponsesHaveNonBlankExpectedReturn() {
    for (String prompt : new String[]{"aggressive", "conservative", "balanced"}) {
      PortfolioResponse r = service.generate(prompt);
      assertFalse(r.expectedReturn().isBlank(),
          "expectedReturn should not be blank for prompt: " + prompt);
    }
  }

  @Test
  void aggressiveExpectedReturnIs12Point4Percent() {
    assertEquals("12.4%", service.generate("aggressive long term young").expectedReturn());
  }

  @Test
  void conservativeExpectedReturnIs6Point1Percent() {
    assertEquals("6.1%", service.generate("conservative safe retire preserv").expectedReturn());
  }

  @Test
  void moderateExpectedReturnIs9Point8Percent() {
    assertEquals("9.8%", service.generate("some random prompt").expectedReturn());
  }
}