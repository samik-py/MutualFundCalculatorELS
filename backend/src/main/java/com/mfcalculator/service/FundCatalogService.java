package com.mfcalculator.service;

import com.mfcalculator.dto.FundOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FundCatalogService {
  private static final List<FundOption> FUNDS = List.of(
      new FundOption("vanguard-500", "Vanguard 500 Index", "VFIAX"),
      new FundOption("fidelity-growth", "Fidelity Growth Company", "FDGRX"),
      new FundOption("trowe-bluechip", "T. Rowe Price Blue Chip", "TRBCX"),
      new FundOption("schwab-total", "Schwab Total Market", "SWTSX"),
      new FundOption("pimco-total", "PIMCO Total Return", "PTTRX")
  );

  private static final Map<String, String> FUND_TICKERS = Map.of(
      "vanguard-500", "VFIAX",
      "fidelity-growth", "FDGRX",
      "trowe-bluechip", "TRBCX",
      "schwab-total", "SWTSX",
      "pimco-total", "PTTRX"
  );
  private static final Map<String, String> TICKER_CANONICAL = Map.of(
      "VFIAX", "VFIAX",
      "FDGRX", "FDGRX",
      "TRBCX", "TRBCX",
      "SWTSX", "SWTSX",
      "PTTRX", "PTTRX"
  );

  public List<FundOption> listFunds() {
    return FUNDS;
  }

  public Optional<String> tickerFor(String fundId) {
    if (fundId == null || fundId.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(FUND_TICKERS.get(fundId));
  }

  public Optional<String> resolveTicker(String fundIdOrTicker) {
    if (fundIdOrTicker == null || fundIdOrTicker.isBlank()) {
      return Optional.empty();
    }
    String fromId = FUND_TICKERS.get(fundIdOrTicker);
    if (fromId != null) {
      return Optional.of(fromId);
    }
    String upper = fundIdOrTicker.trim().toUpperCase();
    String canonical = TICKER_CANONICAL.get(upper);
    if (canonical != null) {
      return Optional.of(canonical);
    }
    return Optional.of(upper);
  }
}
