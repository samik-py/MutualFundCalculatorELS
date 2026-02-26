package com.mfcalculator.service;

import com.mfcalculator.dto.InvestmentRequest;
import com.mfcalculator.dto.InvestmentResponse;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class FinanceService {
  private static final Map<String, Double> FUND_RETURNS = Map.of(
      "vanguard-500", 0.107,
      "fidelity-growth", 0.138,
      "trowe-bluechip", 0.121,
      "schwab-total", 0.104,
      "pimco-total", 0.042
  );

  public InvestmentResponse calculate(InvestmentRequest request) {
    double annualReturn = annualReturnFor(request.fundId());
    double futureValue = request.amount() * Math.pow(1 + annualReturn, request.years());
    double gain = futureValue - request.amount();
    return new InvestmentResponse(futureValue, gain, annualReturn);
  }

  public double annualReturnFor(String fundId) {
    if (fundId == null || fundId.isBlank()) {
      return 0.10;
    }
    return FUND_RETURNS.getOrDefault(fundId, 0.10);
  }
}
