package com.mfcalculator.service;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class NewtonBetaClient implements BetaProvider {
  private static final String API_URL =
      "https://api.newtonanalytics.com/stock-beta/?ticker={ticker}&index={index}&interval={interval}&observations={observations}";
  private static final String INDEX = "^GSPC";
  private static final String INTERVAL = "1mo";
  private static final int OBSERVATIONS = 12;
  private static final Logger logger = LoggerFactory.getLogger(NewtonBetaClient.class);

  private final RestTemplate restTemplate;

  public NewtonBetaClient(RestTemplateBuilder restTemplateBuilder) {
    this.restTemplate = restTemplateBuilder.build();
  }

  @Override
  public double betaFor(String ticker) {
    if (ticker == null || ticker.isBlank()) {
      logger.debug("Beta provider missing ticker");
      return Double.NaN;
    }
    String url = UriComponentsBuilder.fromHttpUrl("https://api.newtonanalytics.com/stock-beta/")
        .queryParam("ticker", ticker)
        .queryParam("index", INDEX)
        .queryParam("interval", INTERVAL)
        .queryParam("observations", OBSERVATIONS)
        .build()
        .toUriString();
    logger.debug("Beta provider request: url={}", url);
    Map<String, Object> response = restTemplate.getForObject(url, Map.class);
    if (response == null) {
      logger.debug("Beta provider returned null response for ticker={}", ticker);
      return Double.NaN;
    }
    double beta = extractBeta(response.get("data"));
    logger.debug("Beta provider result: ticker={}, beta={}", ticker, beta);
    return beta;
  }

  private double extractBeta(Object data) {
    if (data == null) {
      return Double.NaN;
    }
    if (data instanceof Number number) {
      return number.doubleValue();
    }
    if (data instanceof String text) {
      return parseDouble(text);
    }
    if (data instanceof Map<?, ?> map) {
      return extractFromMap(map);
    }
    if (data instanceof List<?> list) {
      return extractFromList(list);
    }
    return Double.NaN;
  }

  private double extractFromMap(Map<?, ?> map) {
    for (String key : List.of("beta", "value", "result")) {
      if (map.containsKey(key)) {
        return extractBeta(map.get(key));
      }
    }
    return Double.NaN;
  }

  private double extractFromList(List<?> list) {
    for (Object entry : list) {
      double value = extractBeta(entry);
      if (!Double.isNaN(value)) {
        return value;
      }
    }
    return Double.NaN;
  }

  private double parseDouble(String text) {
    try {
      return Double.parseDouble(text.trim());
    } catch (NumberFormatException ex) {
      return Double.NaN;
    }
  }
}
