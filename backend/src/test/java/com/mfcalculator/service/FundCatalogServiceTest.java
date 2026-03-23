package com.mfcalculator.service;

import static org.junit.jupiter.api.Assertions.*;

import com.mfcalculator.dto.FundOption;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FundCatalogServiceTest {

  private FundCatalogService service;

  @BeforeEach
  void setUp() {
    service = new FundCatalogService();
  }

  @Test
  void listFundsReturns21Funds() {
    assertEquals(21, service.listFunds().size());
  }

  @Test
  void listFundsContainsVanguard500() {
    boolean found = service.listFunds().stream()
        .anyMatch(f -> f.fundId().equals("vanguard-500"));
    assertTrue(found);
  }

  @Test
  void listFundsAllHaveNonBlankTicker() {
    List<FundOption> funds = service.listFunds();
    for (FundOption f : funds) {
      assertFalse(f.ticker().isBlank(),
          "Fund " + f.fundId() + " has a blank ticker");
    }
  }

  @Test
  void listFundsAllHaveNonBlankName() {
    for (FundOption f : service.listFunds()) {
      assertFalse(f.name().isBlank(),
          "Fund " + f.fundId() + " has a blank name");
    }
  }

  @Test
  void tickerForKnownFundIdReturnsCorrectTicker() {
    assertEquals(Optional.of("VFIAX"), service.tickerFor("vanguard-500"));
  }

  @Test
  void tickerForQqqReturnsQQQ() {
    assertEquals(Optional.of("QQQ"), service.tickerFor("qqq"));
  }

  @Test
  void tickerForUnknownFundIdReturnsEmpty() {
    assertEquals(Optional.empty(), service.tickerFor("does-not-exist"));
  }

  @Test
  void tickerForNullReturnsEmpty() {
    assertEquals(Optional.empty(), service.tickerFor(null));
  }

  @Test
  void tickerForBlankReturnsEmpty() {
    assertEquals(Optional.empty(), service.tickerFor("   "));
  }

  @Test
  void nameForKnownFundIdReturnsCorrectName() {
    Optional<String> name = service.nameFor("vanguard-500");
    assertEquals(Optional.of("Vanguard 500 Index Admiral"), name);
  }

  @Test
  void nameForUnknownFundIdReturnsEmpty() {
    assertEquals(Optional.empty(), service.nameFor("xyz-unknown"));
  }

  @Test
  void resolveTickerFromFundIdReturnsCanonicalTicker() {
    assertEquals(Optional.of("VFIAX"), service.resolveTicker("vanguard-500"));
  }

  @Test
  void resolveTickerFromLowercaseTickerReturnsCanonical() {
    Optional<String> result = service.resolveTicker("vfiax");
    assertTrue(result.isPresent());
    assertEquals("VFIAX", result.get());
  }

  @Test
  void resolveTickerFromUppercaseTickerReturnsSame() {
    Optional<String> result = service.resolveTicker("VFIAX");
    assertEquals(Optional.of("VFIAX"), result);
  }

  @Test
  void resolveTickerForUnknownStringReturnsUppercasedInput() {
    Optional<String> result = service.resolveTicker("mysymbol");
    assertEquals(Optional.of("MYSYMBOL"), result);
  }

  @Test
  void resolveTickerForNullReturnsEmpty() {
    assertEquals(Optional.empty(), service.resolveTicker(null));
  }

  @Test
  void resolveTickerForBlankReturnsEmpty() {
    assertEquals(Optional.empty(), service.resolveTicker(""));
  }

  @Test
  void resolveTickerAllFundIdsHaveValidResolution() {
    for (FundOption f : service.listFunds()) {
      Optional<String> resolved = service.resolveTicker(f.fundId());
      assertTrue(resolved.isPresent(), "resolveTicker returned empty for " + f.fundId());
    }
  }
}
