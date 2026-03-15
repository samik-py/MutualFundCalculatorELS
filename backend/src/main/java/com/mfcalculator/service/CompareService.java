package com.mfcalculator.service;

import com.mfcalculator.dto.CompareRequest;
import com.mfcalculator.dto.CompareResponse;
import com.mfcalculator.dto.FundProjection;
import com.mfcalculator.dto.PortfolioCompareRequest;
import com.mfcalculator.dto.PortfolioCompareResponse;
import com.mfcalculator.dto.PortfolioHolding;
import com.mfcalculator.dto.YearlyDataPoint;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;

@Service
public class CompareService {
  private final FinanceService financeService;
  private final FundCatalogService fundCatalogService;

  public CompareService(FinanceService financeService, FundCatalogService fundCatalogService) {
    this.financeService = financeService;
    this.fundCatalogService = fundCatalogService;
  }

  public CompareResponse compare(CompareRequest request) {
    List<FundProjection> funds = request.fundIds().stream()
        .map(fundId -> {
          double annualReturn = financeService.annualReturnFor(fundId);
          List<YearlyDataPoint> projection = yearlyProjection(request.amount(), annualReturn, request.years());
          String name = fundCatalogService.nameFor(fundId).orElse(fundId);
          return new FundProjection(fundId, name, annualReturn, projection);
        })
        .toList();
    return new CompareResponse(funds, request.amount());
  }

  public PortfolioCompareResponse comparePortfolios(PortfolioCompareRequest request) {
    double returnA = weightedReturn(request.portfolioA());
    double returnB = weightedReturn(request.portfolioB());
    String nameA = blankOrDefault(request.nameA(), "Portfolio A");
    String nameB = blankOrDefault(request.nameB(), "Portfolio B");
    return new PortfolioCompareResponse(
        yearlyProjection(request.amount(), returnA, request.years()),
        yearlyProjection(request.amount(), returnB, request.years()),
        nameA, nameB, returnA, returnB
    );
  }

  private double weightedReturn(List<PortfolioHolding> holdings) {
    double total = holdings.stream().mapToDouble(PortfolioHolding::weight).sum();
    if (total <= 0) return 0;
    return holdings.stream()
        .mapToDouble(h -> (h.weight() / total) * financeService.annualReturnFor(h.fundId()))
        .sum();
  }

  private List<YearlyDataPoint> yearlyProjection(double amount, double rate, int years) {
    return IntStream.rangeClosed(0, years)
        .mapToObj(y -> new YearlyDataPoint(y, amount * Math.exp(rate * y)))
        .toList();
  }

  private String blankOrDefault(String value, String fallback) {
    return (value != null && !value.isBlank()) ? value : fallback;
  }
}
