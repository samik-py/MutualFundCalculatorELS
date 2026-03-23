package com.mfcalculator.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

class FundExpectedReturnServiceExtendedTest {

  private FundExpectedReturnService service() {
    return new FundExpectedReturnService(new RestTemplateBuilder(), 0.10);
  }

  @Test
  void positiveReturnWhenPriceRises() {
    List<?> closes = Arrays.asList(100.0, 110.0, 105.0, 120.0);
    double result = service().computeExpectedReturnFromCloses(closes);
    assertEquals(0.20, result, 1e-9);
  }

  @Test
  void negativeReturnWhenPriceFalls() {
    List<?> closes = Arrays.asList(200.0, 190.0, 185.0, 180.0);
    double result = service().computeExpectedReturnFromCloses(closes);
    assertEquals(-0.10, result, 1e-9);
  }

  @Test
  void zeroReturnWhenStartEqualsEnd() {
    List<?> closes = Arrays.asList(150.0, 160.0, 140.0, 150.0);
    double result = service().computeExpectedReturnFromCloses(closes);
    assertEquals(0.0, result, 1e-9);
  }

  @Test
  void skipsLeadingNullsToFindFirstValid() {
    List<?> closes = Arrays.asList(null, null, 100.0, 130.0);
    double result = service().computeExpectedReturnFromCloses(closes);
    assertEquals(0.30, result, 1e-9);
  }

  @Test
  void skipsTrailingNullsToFindLastValid() {
    List<?> closes = Arrays.asList(100.0, 130.0, null, null);
    double result = service().computeExpectedReturnFromCloses(closes);
    assertEquals(0.30, result, 1e-9);
  }

  @Test
  void skipsNullsAtBothEnds() {
    List<?> closes = Arrays.asList(null, 100.0, 105.0, null);
    double result = service().computeExpectedReturnFromCloses(closes);
    assertEquals(0.05, result, 1e-9);
  }

  @Test
  void returnsNaNForAllNullCloses() {
    List<?> closes = Arrays.asList(null, null, null);
    assertTrue(Double.isNaN(service().computeExpectedReturnFromCloses(closes)));
  }

  @Test
  void returnsNaNForEmptyCloseList() {
    assertTrue(Double.isNaN(service().computeExpectedReturnFromCloses(List.of())));
  }

  @Test
  void returnsNaNWhenStartIsZero() {
    List<?> closes = Arrays.asList(0.0, 100.0);
    assertTrue(Double.isNaN(service().computeExpectedReturnFromCloses(closes)));
  }

  @Test
  void singleElementListReturnsZeroReturn() {
    List<?> closes = Collections.singletonList(100.0);
    double result = service().computeExpectedReturnFromCloses(closes);
    assertEquals(0.0, result, 1e-9);
  }

  @Test
  void returnsConfiguredFallbackWhenNetworkFails() {
    FundExpectedReturnService svc =
        new FundExpectedReturnService(new RestTemplateBuilder(), 0.10);
    double rate = svc.expectedReturnFor("VFIAX");
    assertEquals(0.10, rate, 1e-9);
  }

  @Test
  void returnsCustomFallbackValue() {
    FundExpectedReturnService svc =
        new FundExpectedReturnService(new RestTemplateBuilder(), 0.07);
    double rate = svc.expectedReturnFor("QQQ");
    assertEquals(0.07, rate, 1e-9);
  }

  @Test
  void returnsConfiguredFallbackForNullTicker() {
    double rate = service().expectedReturnFor(null);
    assertEquals(0.10, rate, 1e-9);
  }

  @Test
  void returnsConfiguredFallbackForBlankTicker() {
    double rate = service().expectedReturnFor("  ");
    assertEquals(0.10, rate, 1e-9);
  }
}