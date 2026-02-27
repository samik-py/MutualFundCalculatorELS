package com.mfcalculator.controller;

import com.mfcalculator.dto.FundOption;
import com.mfcalculator.dto.InvestmentRequest;
import com.mfcalculator.dto.InvestmentResponse;
import com.mfcalculator.dto.PortfolioRequest;
import com.mfcalculator.dto.PortfolioResponse;
import com.mfcalculator.service.FinanceService;
import com.mfcalculator.service.FundCatalogService;
import com.mfcalculator.service.PortfolioService;
import jakarta.validation.Valid;
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

  public MutualFundController(
      FinanceService financeService,
      PortfolioService portfolioService,
      FundCatalogService fundCatalogService
  ) {
    this.financeService = financeService;
    this.portfolioService = portfolioService;
    this.fundCatalogService = fundCatalogService;
  }

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
}
