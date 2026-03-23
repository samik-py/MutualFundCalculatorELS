package com.mfcalculator.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

class MarketReturnServiceTest {

  @Test
  void cagrFormulaIsCorrect() {
    double start = 3_000.0;
    double end   = 4_500.0;
    double expected = Math.pow(end / start, 1.0 / 5.0) - 1.0;
    assertEquals(0.08447, expected, 1e-4);
  }

  @Test
  void serviceReturnsFallbackWhenNetworkUnavailable() {
    MarketReturnService service = new MarketReturnService(
        new RestTemplateBuilder(), 0.10);
    double rate = service.marketReturn();
    assertEquals(0.10, rate, 1e-9);
  }

  @Test
  void customFallbackValueIsRespected() {
    MarketReturnService service = new MarketReturnService(
        new RestTemplateBuilder(), 0.07);
    assertEquals(0.07, service.marketReturn(), 1e-9);
  }
}