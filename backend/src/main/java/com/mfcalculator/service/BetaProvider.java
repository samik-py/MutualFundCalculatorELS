package com.mfcalculator.service;

@FunctionalInterface
public interface BetaProvider {
  double betaFor(String ticker);
}
