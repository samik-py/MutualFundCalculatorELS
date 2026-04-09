package com.mfcalculator.service;

import com.mfcalculator.dto.InvestmentRequest;
import com.mfcalculator.dto.InvestmentResponse;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FinanceService {
  private static final double DEFAULT_BETA = 1.0;
  private static final Logger logger = LoggerFactory.getLogger(FinanceService.class);

  private final BetaProvider betaProvider;
  private final FundCatalogService fundCatalogService;
  private final RiskFreeRateProvider riskFreeRateProvider;
  private final FundExpectedReturnProvider expectedReturnProvider;

  public FinanceService(
      BetaProvider betaProvider,
      FundCatalogService fundCatalogService,
      RiskFreeRateProvider riskFreeRateProvider,
      FundExpectedReturnProvider expectedReturnProvider
  ) {
    this.betaProvider = betaProvider;
    this.fundCatalogService = fundCatalogService;
    this.riskFreeRateProvider = riskFreeRateProvider;
    this.expectedReturnProvider = expectedReturnProvider;
  }

  public InvestmentResponse calculate(InvestmentRequest request) {
    logger.info("/api/calculate fundId={} amount=${} years={}",
        request.fundId(), request.amount(), request.years());
    double annualReturn = annualReturnFor(request.fundId());
    double futureValue = request.amount() * Math.exp(annualReturn * request.years());
    double gain = futureValue - request.amount();
    logger.info("/api/calculate result futureValue=${} gain=${} annualReturn={}",
        String.format("%.2f", futureValue), String.format("%.2f", gain), annualReturn);
    return new InvestmentResponse(futureValue, gain, annualReturn);
  }

  public double annualReturnFor(String fundId) {
    Optional<String> ticker = fundCatalogService.resolveTicker(fundId);
    logger.info("[CAPM] resolved ticker={}", ticker.orElse("(none)"));

    double beta = DEFAULT_BETA;
    boolean betaIsFallback = true;
    if (ticker.isPresent()) {
      double candidate = safeBeta(ticker.get());
      if (!Double.isNaN(candidate)) {
        beta = candidate;
        betaIsFallback = false;
      }
    }
    logger.info("[CAPM] beta={} ({})",
        beta,
        betaIsFallback ? "FALLBACK - Newton API failed or NaN" : "live from Newton Analytics");

    double riskFreeRate = riskFreeRateProvider.riskFreeRate();
    logger.info("[CAPM] Rf={} (from FRED DGS10 or fallback)", riskFreeRate);

    double expectedReturn = expectedReturnProvider.expectedReturnFor(ticker.orElse(""));
    logger.info("[CAPM] expectedReturn={} (Yahoo prev-year or fallback)", expectedReturn);

    double annualReturn = riskFreeRate + beta * (expectedReturn - riskFreeRate);
    logger.info("[CAPM] annualReturn = Rf + beta*(E[R]-Rf) = {} + {}*({} - {}) = {}",
        riskFreeRate, beta, expectedReturn, riskFreeRate, annualReturn);

    return annualReturn;
  }

  private double safeBeta(String ticker) {
    try {
      double beta = betaProvider.betaFor(ticker);
      if (Double.isNaN(beta)) {
        logger.debug("Beta provider returned NaN for ticker={}", ticker);
      }
      return beta;
    } catch (RuntimeException ex) {
      logger.debug("Beta provider threw for ticker={}: {}", ticker, ex.getMessage());
      return Double.NaN;
    }
  }
}
