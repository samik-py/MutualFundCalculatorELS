package com.mfcalculator.controller;

import com.mfcalculator.dto.CompareRequest;
import com.mfcalculator.dto.CompareResponse;
import com.mfcalculator.dto.FundOption;
import com.mfcalculator.dto.InvestmentRequest;
import com.mfcalculator.dto.InvestmentResponse;
import com.mfcalculator.dto.MarketIndicatorsResponse;
import com.mfcalculator.dto.MonteCarloRequest;
import com.mfcalculator.dto.MonteCarloResponse;
import com.mfcalculator.dto.PortfolioCompareRequest;
import com.mfcalculator.dto.PortfolioCompareResponse;
import com.mfcalculator.dto.PortfolioRequest;
import com.mfcalculator.dto.PortfolioResponse;
import com.mfcalculator.service.CompareService;
import com.mfcalculator.service.FinanceService;
import com.mfcalculator.service.FundCatalogService;
import com.mfcalculator.service.MarketReturnProvider;
import com.mfcalculator.service.MonteCarloService;
import com.mfcalculator.service.PortfolioService;
import com.mfcalculator.service.RiskFreeRateProvider;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MutualFundController {
  private final FinanceService financeService;
  private final PortfolioService portfolioService;
  private final FundCatalogService fundCatalogService;
  private final CompareService compareService;
  private final MonteCarloService monteCarloService;
  private final RiskFreeRateProvider riskFreeRateProvider;
  private final MarketReturnProvider marketReturnProvider;

  public MutualFundController(
      FinanceService financeService,
      PortfolioService portfolioService,
      FundCatalogService fundCatalogService,
      CompareService compareService,
      MonteCarloService monteCarloService,
      RiskFreeRateProvider riskFreeRateProvider,
      MarketReturnProvider marketReturnProvider
  ) {
    this.financeService = financeService;
    this.portfolioService = portfolioService;
    this.fundCatalogService = fundCatalogService;
    this.compareService = compareService;
    this.monteCarloService = monteCarloService;
    this.riskFreeRateProvider = riskFreeRateProvider;
    this.marketReturnProvider = marketReturnProvider;
  }

  // ── Existing endpoints ────────────────────────────────────────────────────

  @GetMapping("/funds")
  public List<FundOption> funds() {
    return fundCatalogService.listFunds();
  }

  @PostMapping("/calculate")
  public InvestmentResponse calculate(@Valid @RequestBody InvestmentRequest request) {
    return financeService.calculate(request);
  }

  @PostMapping("/ai/portfolio")
  public PortfolioResponse portfolio(@Valid @RequestBody PortfolioRequest request) {
    return portfolioService.generate(request.prompt());
  }

  // ── New endpoints ─────────────────────────────────────────────────────────

  /**
   * Ground truth: live market parameters used as CAPM inputs.
   * Rf from FRED (or fallback), Rm from Yahoo Finance 5yr CAGR (or fallback).
   */
  @GetMapping("/market/indicators")
  public MarketIndicatorsResponse marketIndicators() {
    double rf = riskFreeRateProvider.riskFreeRate();
    double rm = marketReturnProvider.marketReturn();
    return new MarketIndicatorsResponse(
        rf, rm,
        LocalDate.now().toString(),
        "FRED DGS10 (10-Year Treasury Constant Maturity Rate)",
        "Yahoo Finance — S&P 500 (^GSPC) 5-year CAGR"
    );
  }

  /**
   * Compare projected future values for multiple funds side-by-side.
   */
  @PostMapping("/compare")
  public CompareResponse compare(@Valid @RequestBody CompareRequest request) {
    return compareService.compare(request);
  }

  /**
   * Compare two user-built portfolios (weighted fund allocations) over time.
   */
  @PostMapping("/portfolio/compare")
  public PortfolioCompareResponse portfolioCompare(@Valid @RequestBody PortfolioCompareRequest request) {
    return compareService.comparePortfolios(request);
  }

  /**
   * Monte Carlo simulation (Geometric Brownian Motion) for a single fund.
   * Returns percentile confidence bands for predictive modeling visualization.
   */
  @PostMapping("/monte-carlo")
  public MonteCarloResponse monteCarlo(@Valid @RequestBody MonteCarloRequest request) {
    return monteCarloService.simulate(request);
  }
}
