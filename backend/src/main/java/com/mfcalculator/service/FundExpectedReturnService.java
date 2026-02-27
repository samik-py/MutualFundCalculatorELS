package com.mfcalculator.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class FundExpectedReturnService implements FundExpectedReturnProvider {
  private static final Duration CACHE_TTL = Duration.ofHours(24);
  private static final Logger logger = LoggerFactory.getLogger(FundExpectedReturnService.class);

  private final RestTemplate restTemplate;
  private final double fallbackExpectedReturn;

  private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

  public FundExpectedReturnService(
      RestTemplateBuilder restTemplateBuilder,
      @Value("${capm.expectedReturnFallback:0.10}") double fallbackExpectedReturn
  ) {
    this.restTemplate = restTemplateBuilder.build();
    this.fallbackExpectedReturn = fallbackExpectedReturn;
  }

  @Override
  public double expectedReturnFor(String ticker) {
    String key = cacheKey(ticker);
    CacheEntry cachedEntry = cache.get(key);
    if (isCacheFresh(cachedEntry)) {
      logger.debug("Expected return cache hit: ticker={}, rate={}", key, cachedEntry.rate());
      return cachedEntry.rate();
    }

    double fetched = fetchExpectedReturn(ticker);
    if (Double.isNaN(fetched)) {
      double cached = cachedOrFallback(cachedEntry);
      logger.debug("Expected return fetch failed, using cached/fallback rate={}", cached);
      return cached;
    }

    CacheEntry updated = new CacheEntry(fetched, Instant.now());
    cache.put(key, updated);
    logger.debug("Expected return fetched from Yahoo Finance: ticker={}, rate={}", key, fetched);
    return fetched;
  }

  private boolean isCacheFresh(CacheEntry entry) {
    if (entry == null || Double.isNaN(entry.rate())) {
      return false;
    }
    return entry.fetchedAt().plus(CACHE_TTL).isAfter(Instant.now());
  }

  private double cachedOrFallback(CacheEntry entry) {
    if (entry != null && !Double.isNaN(entry.rate())) {
      return entry.rate();
    }
    return fallbackExpectedReturn;
  }

  private double fetchExpectedReturn(String ticker) {
    if (ticker == null || ticker.isBlank()) {
      return Double.NaN;
    }

    int previousYear = LocalDate.now(ZoneOffset.UTC).getYear() - 1;
    long periodStart = startOfYearEpoch(previousYear);
    long periodEnd = startOfYearEpoch(previousYear + 1);

    String url = UriComponentsBuilder
        .fromHttpUrl("https://query1.finance.yahoo.com/v8/finance/chart/{ticker}")
        .queryParam("period1", periodStart)
        .queryParam("period2", periodEnd)
        .queryParam("interval", "1d")
        .queryParam("events", "history")
        .queryParam("includeAdjustedClose", "true")
        .buildAndExpand(ticker)
        .toUriString();

    Map<String, Object> response;
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.add("User-Agent", "Mozilla/5.0");
      logger.debug("Yahoo chart request: url={}", url);
      ResponseEntity<Map> entity =
          restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
      logger.debug("Yahoo chart response: status={}", entity.getStatusCodeValue());
      response = entity.getBody();
    } catch (RestClientResponseException ex) {
      logger.warn(
          "Yahoo chart request failed: status={}, message={}",
          ex.getRawStatusCode(),
          ex.getMessage()
      );
      return Double.NaN;
    } catch (RuntimeException ex) {
      logger.warn("Yahoo chart request failed: message={}", ex.getMessage());
      return Double.NaN;
    }

    List<?> closes = closeSeries(response);
    if (closes.isEmpty()) {
      return Double.NaN;
    }

    return computeExpectedReturnFromCloses(closes);
  }

  double computeExpectedReturnFromCloses(List<?> closes) {
    Double start = firstValidClose(closes);
    Double end = lastValidClose(closes);
    if (start == null || end == null || start <= 0.0) {
      return Double.NaN;
    }
    return (end - start) / start;
  }

  private long startOfYearEpoch(int year) {
    ZonedDateTime start = ZonedDateTime.of(LocalDate.of(year, 1, 1), java.time.LocalTime.MIDNIGHT, ZoneOffset.UTC);
    return start.toEpochSecond();
  }

  private String cacheKey(String ticker) {
    if (ticker == null) {
      return "";
    }
    return ticker.trim().toUpperCase();
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

  private record CacheEntry(double rate, Instant fetchedAt) {}
}
