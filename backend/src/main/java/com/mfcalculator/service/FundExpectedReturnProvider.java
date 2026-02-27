package com.mfcalculator.service;

@FunctionalInterface
public interface FundExpectedReturnProvider {
  double expectedReturnFor(String ticker);
}
