package com.mfcalculator.service;

import static org.junit.jupiter.api.Assertions.*;

import com.mfcalculator.dto.CompareRequest;
import com.mfcalculator.dto.CompareResponse;
import com.mfcalculator.dto.FundProjection;
import com.mfcalculator.dto.PortfolioCompareRequest;
import com.mfcalculator.dto.PortfolioCompareResponse;
import com.mfcalculator.dto.PortfolioHolding;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompareServiceTest {

  private FundCatalogService fundCatalogService;
  private FinanceService financeService;
  private CompareService compareService;

  @BeforeEach
  void setUp() {
    fundCatalogService = new FundCatalogService();
    financeService = new FinanceService(
        ticker -> 1.0,
        fundCatalogService,
        () -> 0.04,
        ticker -> 0.10
    );
    compareService = new CompareService(financeService, fundCatalogService);
  }

  @Test
  void compareReturnsFundProjectionForEachRequestedFund() {
    CompareRequest request = new CompareRequest(
        List.of("vanguard-500", "qqq"), 10_000.0, 5);
    CompareResponse response = compareService.compare(request);

    assertEquals(2, response.funds().size());
    assertEquals(10_000.0, response.initialAmount(), 1e-9);
  }

  @Test
  void compareProjectionHasYearsPlusOneDataPoints() {
    CompareRequest request = new CompareRequest(List.of("vanguard-500"), 5_000.0, 10);
    CompareResponse response = compareService.compare(request);

    FundProjection projection = response.funds().get(0);
    assertEquals(11, projection.projection().size());
  }

  @Test
  void compareYear0ValueEqualsInitialAmount() {
    CompareRequest request = new CompareRequest(List.of("vanguard-500"), 8_000.0, 3);
    CompareResponse response = compareService.compare(request);

    double year0 = response.funds().get(0).projection().get(0).value();
    assertEquals(8_000.0, year0, 1e-9);
  }

  @Test
  void compareUsesCorrectContinuousCompounding() {
    double amount = 10_000.0;
    int years = 10;
    double rate = 0.10;
    double expectedFinalValue = amount * Math.exp(rate * years);

    CompareRequest request = new CompareRequest(List.of("vanguard-500"), amount, years);
    CompareResponse response = compareService.compare(request);

    double actualFinalValue = response.funds().get(0).projection().get(years).value();
    assertEquals(expectedFinalValue, actualFinalValue, 1e-6);
  }

  @Test
  void compareResolvesHumanReadableFundName() {
    CompareRequest request = new CompareRequest(List.of("vanguard-500"), 1_000.0, 1);
    CompareResponse response = compareService.compare(request);

    String name = response.funds().get(0).name();
    assertEquals("Vanguard 500 Index Admiral", name);
  }

  @Test
  void compareUsesUnknownFundIdAsNameWhenNotInCatalog() {
    CompareRequest request = new CompareRequest(List.of("unknown-fund-xyz"), 1_000.0, 1);
    CompareResponse response = compareService.compare(request);

    assertEquals("unknown-fund-xyz", response.funds().get(0).name());
  }

  @Test
  void comparePortfoliosReturnsProjectionsForBothPortfolios() {
    List<PortfolioHolding> portA = List.of(new PortfolioHolding("vanguard-500", 100.0));
    List<PortfolioHolding> portB = List.of(new PortfolioHolding("pimco-total", 100.0));

    PortfolioCompareRequest request = new PortfolioCompareRequest(
        portA, portB, 10_000.0, 5, "A", "B");
    PortfolioCompareResponse response = compareService.comparePortfolios(request);

    assertNotNull(response.portfolioA());
    assertNotNull(response.portfolioB());
    assertEquals(6, response.portfolioA().size());
    assertEquals(6, response.portfolioB().size());
  }

  @Test
  void comparePortfoliosUsesDefaultNamesWhenBlank() {
    List<PortfolioHolding> portA = List.of(new PortfolioHolding("vanguard-500", 50.0));
    List<PortfolioHolding> portB = List.of(new PortfolioHolding("qqq", 50.0));

    PortfolioCompareRequest request = new PortfolioCompareRequest(
        portA, portB, 5_000.0, 1, "", null);
    PortfolioCompareResponse response = compareService.comparePortfolios(request);

    assertEquals("Portfolio A", response.nameA());
    assertEquals("Portfolio B", response.nameB());
  }

  @Test
  void comparePortfoliosPreservesCustomNames() {
    List<PortfolioHolding> portA = List.of(new PortfolioHolding("vanguard-500", 100.0));
    List<PortfolioHolding> portB = List.of(new PortfolioHolding("qqq", 100.0));

    PortfolioCompareRequest request = new PortfolioCompareRequest(
        portA, portB, 1_000.0, 1, "Growth", "Bond");
    PortfolioCompareResponse response = compareService.comparePortfolios(request);

    assertEquals("Growth", response.nameA());
    assertEquals("Bond", response.nameB());
  }

  @Test
  void comparePortfoliosNormalizesWeightsCorrectly() {
    List<PortfolioHolding> portA = List.of(
        new PortfolioHolding("vanguard-500", 30.0),
        new PortfolioHolding("vanguard-500", 70.0)
    );
    List<PortfolioHolding> portB = List.of(new PortfolioHolding("vanguard-500", 100.0));

    PortfolioCompareRequest request = new PortfolioCompareRequest(
        portA, portB, 10_000.0, 10, "A", "B");
    PortfolioCompareResponse response = compareService.comparePortfolios(request);

    double finalA = response.portfolioA().get(10).value();
    double finalB = response.portfolioB().get(10).value();
    assertEquals(finalA, finalB, 1e-6);
  }

  @Test
  void comparePortfoliosYear0EqualsInitialAmount() {
    List<PortfolioHolding> portA = List.of(new PortfolioHolding("vanguard-500", 100.0));
    List<PortfolioHolding> portB = List.of(new PortfolioHolding("qqq", 100.0));

    PortfolioCompareRequest request = new PortfolioCompareRequest(
        portA, portB, 12_345.0, 5, "A", "B");
    PortfolioCompareResponse response = compareService.comparePortfolios(request);

    assertEquals(12_345.0, response.portfolioA().get(0).value(), 1e-9);
    assertEquals(12_345.0, response.portfolioB().get(0).value(), 1e-9);
  }
}
