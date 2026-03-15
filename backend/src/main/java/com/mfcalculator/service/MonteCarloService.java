package com.mfcalculator.service;

import com.mfcalculator.dto.MonteCarloRequest;
import com.mfcalculator.dto.MonteCarloResponse;
import com.mfcalculator.dto.PercentileSeries;
import com.mfcalculator.dto.YearlyDataPoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Monte Carlo simulation using Geometric Brownian Motion (GBM).
 *
 * Model: S(t+1) = S(t) * exp((mu - 0.5 * sigma^2) + sigma * Z)
 * where Z ~ N(0,1) and sigma = beta * market_annual_volatility.
 *
 * Returns percentile bands (5th, 25th, 50th, 75th, 95th) at each year for
 * confidence-interval visualization — this is the predictive model layer on
 * top of the deterministic CAPM projection.
 */
@Service
public class MonteCarloService {
  private static final double MARKET_ANNUAL_VOLATILITY = 0.165; // historical S&P 500 ~16–17%
  private static final double DEFAULT_BETA = 1.0;
  private static final int DEFAULT_SIMULATIONS = 500;
  private static final int MAX_SIMULATIONS = 5000;
  private static final int[] PERCENTILES = {5, 25, 50, 75, 95};
  private static final String[] LABELS = {
      "5th percentile", "25th percentile", "Median (50th)", "75th percentile", "95th percentile"
  };
  private static final Logger logger = LoggerFactory.getLogger(MonteCarloService.class);

  private final FinanceService financeService;
  private final BetaProvider betaProvider;
  private final FundCatalogService fundCatalogService;

  public MonteCarloService(
      FinanceService financeService,
      BetaProvider betaProvider,
      FundCatalogService fundCatalogService
  ) {
    this.financeService = financeService;
    this.betaProvider = betaProvider;
    this.fundCatalogService = fundCatalogService;
  }

  public MonteCarloResponse simulate(MonteCarloRequest request) {
    int simCount = clampSimulations(request.simulations());
    double mu = financeService.annualReturnFor(request.fundId());

    double beta = resolveBeta(request.fundId());
    double sigma = beta * MARKET_ANNUAL_VOLATILITY;

    logger.debug("Monte Carlo: fundId={}, mu={}, beta={}, sigma={}, sims={}",
        request.fundId(), mu, beta, sigma, simCount);

    double[][] paths = runSimulations(request.amount(), mu, sigma, request.years(), simCount);
    List<PercentileSeries> series = buildPercentileSeries(paths, request.years(), simCount);

    return new MonteCarloResponse(series, mu, sigma, beta);
  }

  private double[][] runSimulations(double amount, double mu, double sigma, int years, int simCount) {
    Random rng = new Random();
    double[][] paths = new double[simCount][years + 1];
    double drift = mu - 0.5 * sigma * sigma;
    for (int s = 0; s < simCount; s++) {
      paths[s][0] = amount;
      for (int y = 1; y <= years; y++) {
        paths[s][y] = paths[s][y - 1] * Math.exp(drift + sigma * rng.nextGaussian());
      }
    }
    return paths;
  }

  private List<PercentileSeries> buildPercentileSeries(double[][] paths, int years, int simCount) {
    List<PercentileSeries> series = new ArrayList<>();
    for (int pi = 0; pi < PERCENTILES.length; pi++) {
      List<YearlyDataPoint> points = new ArrayList<>();
      for (int y = 0; y <= years; y++) {
        points.add(new YearlyDataPoint(y, percentileAt(paths, y, PERCENTILES[pi], simCount)));
      }
      series.add(new PercentileSeries(LABELS[pi], PERCENTILES[pi], points));
    }
    return series;
  }

  private double percentileAt(double[][] paths, int year, int percentile, int simCount) {
    double[] col = new double[simCount];
    for (int s = 0; s < simCount; s++) {
      col[s] = paths[s][year];
    }
    Arrays.sort(col);
    int idx = (int) Math.round((percentile / 100.0) * (simCount - 1));
    return col[Math.max(0, Math.min(idx, simCount - 1))];
  }

  private double resolveBeta(String fundId) {
    Optional<String> ticker = fundCatalogService.resolveTicker(fundId);
    if (ticker.isEmpty()) return DEFAULT_BETA;
    try {
      double beta = betaProvider.betaFor(ticker.get());
      return Double.isNaN(beta) ? DEFAULT_BETA : beta;
    } catch (RuntimeException ex) {
      logger.debug("Beta lookup failed for {}: {}", fundId, ex.getMessage());
      return DEFAULT_BETA;
    }
  }

  private int clampSimulations(Integer requested) {
    if (requested == null || requested <= 0) return DEFAULT_SIMULATIONS;
    return Math.min(requested, MAX_SIMULATIONS);
  }
}
