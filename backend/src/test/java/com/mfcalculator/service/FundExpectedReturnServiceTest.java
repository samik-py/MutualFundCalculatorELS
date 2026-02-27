package com.mfcalculator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

class FundExpectedReturnServiceTest {
  @Test
  void computesExpectedReturnFromFirstAndLastTradingDay() {
    FundExpectedReturnService service =
        new FundExpectedReturnService(new RestTemplateBuilder(), 0.10);

    List<?> closes = Arrays.asList(null, 100.0, 101.5, null, 105.0);
    double expectedReturn = service.computeExpectedReturnFromCloses(closes);

    assertEquals(0.05, expectedReturn, 1e-9);
  }

  @Test
  void returnsNaNWhenNoValidCloses() {
    FundExpectedReturnService service =
        new FundExpectedReturnService(new RestTemplateBuilder(), 0.10);

    List<?> closes = Arrays.asList(null, null);
    double expectedReturn = service.computeExpectedReturnFromCloses(closes);

    assertTrue(Double.isNaN(expectedReturn));
  }
}
