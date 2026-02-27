package com.mfcalculator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mfcalculator.dto.InvestmentRequest;
import com.mfcalculator.dto.InvestmentResponse;
import org.junit.jupiter.api.Test;

class FinanceServiceTest {
  private final FundCatalogService fundCatalogService = new FundCatalogService();

  @Test
  void calculatesCapmAnnualReturnFromBeta() {
    FinanceService financeService = new FinanceService(
        ticker -> 1.2,
        fundCatalogService,
        () -> 0.04,
        ticker -> 0.10
    );

    InvestmentRequest request = new InvestmentRequest("vanguard-500", 10000.0, 10);
    InvestmentResponse response = financeService.calculate(request);
    double expectedAnnualReturn = 0.04 + 1.2 * (0.10 - 0.04);
    double expectedFutureValue = 10000.0 * Math.exp(expectedAnnualReturn * 10);
    assertEquals(expectedFutureValue, response.futureValue(), 1e-6);
    assertEquals(expectedFutureValue - 10000.0, response.gain(), 1e-6);
    assertEquals(expectedAnnualReturn, response.annualReturn(), 1e-9);
  }

  @Test
  void fallsBackToDefaultBetaWhenProviderReturnsNaN() {
    FinanceService financeService = new FinanceService(
        ticker -> Double.NaN,
        fundCatalogService,
        () -> 0.04,
        ticker -> 0.10
    );

    double expectedAnnualReturn = 0.04 + 1.0 * (0.10 - 0.04);
    double annualReturn = financeService.annualReturnFor("vanguard-500");
    assertEquals(expectedAnnualReturn, annualReturn, 1e-9);
  }
}
