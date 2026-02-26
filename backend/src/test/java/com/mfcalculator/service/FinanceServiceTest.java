package com.mfcalculator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mfcalculator.dto.InvestmentRequest;
import com.mfcalculator.dto.InvestmentResponse;
import org.junit.jupiter.api.Test;

class FinanceServiceTest {
  private final FinanceService financeService = new FinanceService();

  @Test
  void usesDefaultReturnForUnknownFund() {
    double rate = financeService.annualReturnFor("unknown");
    assertEquals(0.10, rate, 1e-9);
  }

  @Test
  void calculatesCompoundGrowth() {
    InvestmentRequest request = new InvestmentRequest("vanguard-500", 10000.0, 10);
    InvestmentResponse response = financeService.calculate(request);
    double expectedFutureValue = 10000.0 * Math.pow(1 + 0.107, 10);
    assertEquals(expectedFutureValue, response.futureValue(), 1e-6);
    assertEquals(expectedFutureValue - 10000.0, response.gain(), 1e-6);
    assertEquals(0.107, response.annualReturn(), 1e-9);
  }
}
