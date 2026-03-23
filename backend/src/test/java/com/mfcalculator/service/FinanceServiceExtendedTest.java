package com.mfcalculator.service;

import static org.junit.jupiter.api.Assertions.*;

import com.mfcalculator.dto.InvestmentRequest;
import com.mfcalculator.dto.InvestmentResponse;
import org.junit.jupiter.api.Test;

class FinanceServiceExtendedTest {

  private final FundCatalogService fundCatalogService = new FundCatalogService();

  @Test
  void annualReturnForUsesCorrectCapmFormula() {
    double beta = 1.5;
    double rf   = 0.03;
    double rm   = 0.09;
    double expected = rf + beta * (rm - rf);

    FinanceService svc = new FinanceService(
        ticker -> beta, fundCatalogService, () -> rf, ticker -> rm);

    assertEquals(expected, svc.annualReturnFor("vanguard-500"), 1e-9);
  }

  @Test
  void annualReturnForUnknownFundIdFallsBackToDefaultBeta() {
    double rf = 0.04;
    double rm = 0.10;
    double expected = rf + 1.0 * (rm - rf);

    FinanceService svc = new FinanceService(
        ticker -> Double.NaN, fundCatalogService, () -> rf, ticker -> rm);

    assertEquals(expected, svc.annualReturnFor("vanguard-500"), 1e-9);
  }

  @Test
  void annualReturnForBetaProviderThrowingFallsBackToDefaultBeta() {
    double rf = 0.04;
    double rm = 0.10;
    double expected = rf + 1.0 * (rm - rf);

    FinanceService svc = new FinanceService(
        ticker -> { throw new RuntimeException("network error"); },
        fundCatalogService, () -> rf, ticker -> rm);

    assertEquals(expected, svc.annualReturnFor("vanguard-500"), 1e-9);
  }

  @Test
  void annualReturnForBondFundWithBetaLessThanOne() {
    double beta = 0.2;
    double rf   = 0.04;
    double rm   = 0.10;
    double expected = rf + beta * (rm - rf);

    FinanceService svc = new FinanceService(
        ticker -> beta, fundCatalogService, () -> rf, ticker -> rm);

    assertEquals(expected, svc.annualReturnFor("pimco-total"), 1e-9);
  }

  @Test
  void calculateFutureValueGrowsWithTime() {
    FinanceService svc = new FinanceService(
        ticker -> 1.0, fundCatalogService, () -> 0.04, ticker -> 0.10);

    double fv5  = svc.calculate(new InvestmentRequest("vanguard-500", 10_000.0, 5)).futureValue();
    double fv10 = svc.calculate(new InvestmentRequest("vanguard-500", 10_000.0, 10)).futureValue();

    assertTrue(fv10 > fv5, "Longer horizon should produce a higher future value");
  }

  @Test
  void calculateGainEqualsAndFutureValueMinusPrincipal() {
    FinanceService svc = new FinanceService(
        ticker -> 1.0, fundCatalogService, () -> 0.04, ticker -> 0.10);

    InvestmentResponse response = svc.calculate(new InvestmentRequest("vanguard-500", 5_000.0, 7));
    assertEquals(response.futureValue() - 5_000.0, response.gain(), 1e-6);
  }

  @Test
  void calculateWithZeroYearsReturnsPrincipalUnchanged() {
    FinanceService svc = new FinanceService(
        ticker -> 1.0, fundCatalogService, () -> 0.04, ticker -> 0.10);

    InvestmentResponse response = svc.calculate(new InvestmentRequest("vanguard-500", 8_000.0, 0));
    assertEquals(8_000.0, response.futureValue(), 1e-9);
    assertEquals(0.0,     response.gain(),        1e-9);
  }

  @Test
  void calculateAnnualReturnIsReturnedInResponse() {
    double beta = 1.2;
    double rf   = 0.04;
    double rm   = 0.10;
    double expectedRate = rf + beta * (rm - rf);

    FinanceService svc = new FinanceService(
        ticker -> beta, fundCatalogService, () -> rf, ticker -> rm);

    InvestmentResponse response = svc.calculate(new InvestmentRequest("vanguard-500", 1_000.0, 1));
    assertEquals(expectedRate, response.annualReturn(), 1e-9);
  }

  @Test
  void calculateUsesContinuousCompoundingNotSimple() {
    double rate   = 0.10;
    double amount = 10_000.0;
    int    years  = 10;

    FinanceService svc = new FinanceService(
        ticker -> 1.0, fundCatalogService, () -> 0.04, ticker -> 0.10);

    double actual     = svc.calculate(new InvestmentRequest("vanguard-500", amount, years)).futureValue();
    double continuous = amount * Math.exp(rate * years);
    double simple     = amount * Math.pow(1 + rate, years);

    assertEquals(continuous, actual, 1e-6);
    assertNotEquals(simple, actual, 1.0);
  }
}