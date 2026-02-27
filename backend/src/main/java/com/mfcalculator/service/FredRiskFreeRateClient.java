package com.mfcalculator.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Service
public class FredRiskFreeRateClient implements RiskFreeRateProvider {
  private static final Duration CACHE_TTL = Duration.ofHours(24);
  private static final Logger logger = LoggerFactory.getLogger(FredRiskFreeRateClient.class);

  private final RestTemplate restTemplate;
  private final String baseUrl;
  private final String apiKey;
  private final double fallbackRiskFreeRate;

  private volatile Instant lastFetchedAt;
  private volatile double cachedRate = Double.NaN;

  public FredRiskFreeRateClient(
      RestTemplateBuilder restTemplateBuilder,
      @Value("${fred.baseUrl:https://api.stlouisfed.org/fred}") String baseUrl,
      @Value("${fred.apiKey:${FRED_API_KEY:}}") String apiKey,
      @Value("${capm.riskFreeFallback:0.04}") double fallbackRiskFreeRate
  ) {
    this.restTemplate = restTemplateBuilder.build();
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.fallbackRiskFreeRate = fallbackRiskFreeRate;
  }

  @Override
  public double riskFreeRate() {
    if (apiKey == null || apiKey.isBlank()) {
      logger.debug("Risk-free rate fallback: missing FRED apiKey, rate={}", fallbackRiskFreeRate);
      return fallbackRiskFreeRate;
    }

    if (isCacheFresh()) {
      logger.debug("Risk-free rate cache hit: rate={}", cachedRate);
      return cachedRate;
    }

    double fetched = fetchRiskFreeRate();
    if (Double.isNaN(fetched)) {
      double cached = cachedRateOrDefault();
      logger.debug("Risk-free rate fetch failed, using cached/fallback rate={}", cached);
      return cached;
    }

    cachedRate = fetched;
    lastFetchedAt = Instant.now();
    logger.debug("Risk-free rate fetched from FRED: rate={}", cachedRate);
    return cachedRate;
  }

  private boolean isCacheFresh() {
    if (lastFetchedAt == null || Double.isNaN(cachedRate)) {
      return false;
    }
    return lastFetchedAt.plus(CACHE_TTL).isAfter(Instant.now());
  }

  private double cachedRateOrDefault() {
    if (!Double.isNaN(cachedRate)) {
      return cachedRate;
    }
    return fallbackRiskFreeRate;
  }

  private double fetchRiskFreeRate() {
    String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
        .path("/series/observations")
        .queryParam("series_id", "DGS10")
        .queryParam("api_key", apiKey)
        .queryParam("file_type", "json")
        .queryParam("sort_order", "desc")
        .queryParam("limit", "1")
        .build()
        .toUriString();

    Map<String, Object> response;
    try {
      logger.debug("FRED request: url={}", url);
      ResponseEntity<Map> entity = restTemplate.getForEntity(url, Map.class);
      logger.debug("FRED response: status={}", entity.getStatusCodeValue());
      response = entity.getBody();
    } catch (RestClientResponseException ex) {
      logger.warn(
          "FRED request failed: status={}, message={}",
          ex.getRawStatusCode(),
          ex.getMessage()
      );
      return Double.NaN;
    } catch (RuntimeException ex) {
      logger.warn("FRED request failed: message={}", ex.getMessage());
      return Double.NaN;
    }

    if (response == null) {
      return Double.NaN;
    }
    Object observations = response.get("observations");
    if (!(observations instanceof List<?> list) || list.isEmpty()) {
      return Double.NaN;
    }
    Object first = list.get(0);
    if (!(first instanceof Map<?, ?> map)) {
      return Double.NaN;
    }
    Object value = map.get("value");
    if (value == null) {
      return Double.NaN;
    }
    String text = value.toString().trim();
    if (text.isEmpty() || ".".equals(text)) {
      return Double.NaN;
    }
    try {
      double percent = Double.parseDouble(text);
      return percent / 100.0;
    } catch (NumberFormatException ex) {
      return Double.NaN;
    }
  }
}
