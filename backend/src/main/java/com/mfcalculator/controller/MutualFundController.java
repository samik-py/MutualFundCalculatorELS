package com.mfcalculator.controller;

import com.mfcalculator.dto.InvestmentRequest;
import com.mfcalculator.dto.InvestmentResponse;
import com.mfcalculator.dto.PortfolioRequest;
import com.mfcalculator.dto.PortfolioResponse;
import com.mfcalculator.service.FinanceService;
import com.mfcalculator.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MutualFundController {
  private final FinanceService financeService;
  private final PortfolioService portfolioService;

  public MutualFundController(FinanceService financeService, PortfolioService portfolioService) {
    this.financeService = financeService;
    this.portfolioService = portfolioService;
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
