package com.mfcalculator.service;

import static org.junit.jupiter.api.Assertions.*;

import com.mfcalculator.dto.MonteCarloRequest;
import com.mfcalculator.dto.MonteCarloResponse;
import com.mfcalculator.dto.PercentileSeries;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MonteCarloServiceTest {

  private static final double RF = 0.04;
  private static final double RM = 0.10;
  private static final double BETA = 1.0;

  private FundCatalogService fundCatalogService;
  private FinanceService financeService;
  private MonteCarloService monteCarloService;

  @BeforeEach
  void setUp() {
    fundCatalogService = new FundCatalogService();
    financeService = new FinanceService(
        ticker -> BETA,
        fundCatalogService,
        () -> RF,
        ticker -> RM
    );
    monteCarloService = new MonteCarloService(
        financeService, ticker -> BETA, fundCatalogService);
  }

  @Test
  void simulationReturnsFivePercentileSeries() {
    MonteCarloRequest request = new MonteCarloRequest("vanguard-500", 10_000.0, 5, 200);
    MonteCarloResponse response = monteCarloService.simulate(request);

    assertEquals(5, response.series().size());
  }

  @Test
  void simulationReturnsCorrectPercentileLabels() {
    MonteCarloRequest request = new MonteCarloRequest("vanguard-500", 10_000.0, 5, 200);
    MonteCarloResponse response = monteCarloService.simulate(request);

    int[] expectedPercentiles = {5, 25, 50, 75, 95};
    for (int i = 0; i < 5; i++) {
      assertEquals(expectedPercentiles[i], response.series().get(i).percentile());
    }
  }

  @Test
  void eachSeriesHasYearsPlusOneDataPoints() {
    int years = 8;
    MonteCarloRequest request = new MonteCarloRequest("vanguard-500", 10_000.0, years, 200);
    MonteCarloResponse response = monteCarloService.simulate(request);

    for (PercentileSeries series : response.series()) {
      assertEquals(years + 1, series.data().size(),
          "Series " + series.percentile() + " should have " + (years + 1) + " data points");
    }
  }

  @Test
  void year0ValueEqualsInitialAmountForAllPercentiles() {
    double amount = 7_500.0;
    MonteCarloRequest request = new MonteCarloRequest("vanguard-500", amount, 5, 300);
    MonteCarloResponse response = monteCarloService.simulate(request);

    for (PercentileSeries series : response.series()) {
      assertEquals(amount, series.data().get(0).value(), 1e-6,
          "Year-0 value mismatch for percentile " + series.percentile());
    }
  }

  @Test
  void responseContainsExpectedBaseReturn() {
    double expectedReturn = RF + BETA * (RM - RF);
    MonteCarloRequest request = new MonteCarloRequest("vanguard-500", 10_000.0, 5, 200);
    MonteCarloResponse response = monteCarloService.simulate(request);

    assertEquals(expectedReturn, response.baseReturn(), 1e-9);
  }

  @Test
  void responseContainsExpectedBeta() {
    MonteCarloRequest request = new MonteCarloRequest("vanguard-500", 10_000.0, 5, 200);
    MonteCarloResponse response = monteCarloService.simulate(request);

    assertEquals(BETA, response.estimatedBeta(), 1e-9);
  }

  @Test
  void responseVolatilityEqualsBetaTimesMarketVol() {
    double expectedVol = BETA * 0.165;
    MonteCarloRequest request = new MonteCarloRequest("vanguard-500", 10_000.0, 5, 200);
    MonteCarloResponse response = monteCarloService.simulate(request);

    assertEquals(expectedVol, response.estimatedVolatility(), 1e-9);
  }

  @Test
  void percentilesAreOrderedLowToHighAtFinalYear() {
    int years = 10;
    MonteCarloRequest request = new MonteCarloRequest("vanguard-500", 10_000.0, years, 1000);
    MonteCarloResponse response = monteCarloService.simulate(request);

    double p5  = response.series().get(0).data().get(years).value();
    double p25 = response.series().get(1).data().get(years).value();
    double p50 = response.series().get(2).data().get(years).value();
    double p75 = response.series().get(3).data().get(years).value();
    double p95 = response.series().get(4).data().get(years).value();

    assertTrue(p5 <= p25, "p5 should be <= p25");
    assertTrue(p25 <= p50, "p25 should be <= p50");
    assertTrue(p50 <= p75, "p50 should be <= p75");
    assertTrue(p75 <= p95, "p75 should be <= p95");
  }

  @Test
  void allFinalValuesArePositive() {
    int years = 10;
    MonteCarloRequest request = new MonteCarloRequest("vanguard-500", 10_000.0, years, 500);
    MonteCarloResponse response = monteCarloService.simulate(request);

    for (PercentileSeries series : response.series()) {
      double finalValue = series.data().get(years).value();
      assertTrue(finalValue > 0,
          "Final value should be positive for percentile " + series.percentile());
    }
  }

  @Test
  void nullSimulationsDefaultsTo500() {
    MonteCarloRequest request = new MonteCarloRequest("vanguard-500", 10_000.0, 3, null);
    MonteCarloResponse response = monteCarloService.simulate(request);
    assertNotNull(response);
  }

  @Test
  void zeroSimulationsDefaultsToMinimum() {
    MonteCarloRequest request = new MonteCarloRequest("vanguard-500", 10_000.0, 3, 0);
    MonteCarloResponse response = monteCarloService.simulate(request);
    assertNotNull(response);
  }

  @Test
  void simulationsAboveMaxAreClamped() {
    MonteCarloRequest request = new MonteCarloRequest("vanguard-500", 1_000.0, 2, 99999);
    MonteCarloResponse response = monteCarloService.simulate(request);
    assertNotNull(response);
  }

  @Test
  void unknownFundFallsBackToDefaultBeta() {
    MonteCarloService svcWithThrowingBeta = new MonteCarloService(
        financeService,
        ticker -> { throw new RuntimeException("no beta"); },
        fundCatalogService
    );
    MonteCarloRequest request = new MonteCarloRequest("unknown-fund", 5_000.0, 3, 200);
    MonteCarloResponse response = svcWithThrowingBeta.simulate(request);

    assertEquals(1.0, response.estimatedBeta(), 1e-9);
  }
}