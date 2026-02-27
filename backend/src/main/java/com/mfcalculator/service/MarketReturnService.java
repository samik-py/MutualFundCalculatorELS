package com.mfcalculator.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MarketReturnService implements MarketReturnProvider {
  private static final Duration CACHE_TTL = Duration.ofHours(24);
  private static final double YEARS = 5.0;
  private static final String SP500_CHART_URL =
      "https://query1.finance.yahoo.com/v8/finance/chart/%5EGSPC?range=5y&interval=1mo";
  private static final Logger logger = LoggerFactory.getLogger(MarketReturnService.class);

  private final RestTemplate restTemplate;
  private final double fallbackMarketReturn;

  private volatile Instant lastFetchedAt;
  private volatile double cachedRate = Double.NaN;

  public MarketReturnService(
      RestTemplateBuilder restTemplateBuilder,
      @Value("${capm.marketReturn5y:0.10}") double fallbackMarketReturn
  ) {
    this.restTemplate = restTemplateBuilder.build();
    this.fallbackMarketReturn = fallbackMarketReturn;
  }

  @Override
  public double marketReturn() {
    if (isCacheFresh()) {
      logger.debug("Market return cache hit: rate={}", cachedRate);
      return cachedRate;
    }

    double fetched = fetchSp500Cagr();
    if (Double.isNaN(fetched)) {
      double cached = cachedOrFallback();
      logger.debug("Market return fetch failed, using cached/fallback rate={}", cached);
      return cached;
    }

    cachedRate = fetched;
    lastFetchedAt = Instant.now();
    logger.debug("Market return fetched from Yahoo Finance: rate={}", cachedRate);
    return cachedRate;
  }

  private boolean isCacheFresh() {
    if (lastFetchedAt == null || Double.isNaN(cachedRate)) {
      return false;
    }
    return lastFetchedAt.plus(CACHE_TTL).isAfter(Instant.now());
  }

  private double cachedOrFallback() {
    if (!Double.isNaN(cachedRate)) {
      return cachedRate;
    }
    return fallbackMarketReturn;
  }

  private double fetchSp500Cagr() {
    Map<String, Object> response;
    try {
      response = restTemplate.getForObject(SP500_CHART_URL, Map.class);
    } catch (RuntimeException ex) {
      return Double.NaN;
    }

    List<?> closes = closeSeries(response);
    if (closes.isEmpty()) {
      return Double.NaN;
    }

    Double start = firstValidClose(closes);
    Double end = lastValidClose(closes);
    if (start == null || end == null || start <= 0.0 || end <= 0.0) {
      return Double.NaN;
    }
    return Math.pow(end / start, 1.0 / YEARS) - 1.0;
  }

  private List<?> closeSeries(Map<String, Object> response) {
    if (response == null) {
      return List.of();
    }
    Object chart = response.get("chart");
    if (!(chart instanceof Map<?, ?> chartMap)) {
      return List.of();
    }
    Object result = chartMap.get("result");
    if (!(result instanceof List<?> resultList) || resultList.isEmpty()) {
      return List.of();
    }
    Object first = resultList.get(0);
    if (!(first instanceof Map<?, ?> firstResult)) {
      return List.of();
    }
    Object indicators = firstResult.get("indicators");
    if (!(indicators instanceof Map<?, ?> indicatorsMap)) {
      return List.of();
    }
    Object quote = indicatorsMap.get("quote");
    if (!(quote instanceof List<?> quoteList) || quoteList.isEmpty()) {
      return List.of();
    }
    Object quoteEntry = quoteList.get(0);
    if (!(quoteEntry instanceof Map<?, ?> quoteMap)) {
      return List.of();
    }
    Object close = quoteMap.get("close");
    if (!(close instanceof List<?> closeList)) {
      return List.of();
    }
    return closeList;
  }

  private Double firstValidClose(List<?> closes) {
    for (Object close : closes) {
      if (close instanceof Number number) {
        return number.doubleValue();
      }
    }
    return null;
  }

  private Double lastValidClose(List<?> closes) {
    for (int i = closes.size() - 1; i >= 0; i--) {
      Object close = closes.get(i);
      if (close instanceof Number number) {
        return number.doubleValue();
      }
    }
    return null;
  }
}
