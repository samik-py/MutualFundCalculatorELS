package com.mfcalculator.service;

import com.mfcalculator.dto.FundOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FundCatalogService {
  private static final List<FundOption> FUNDS = List.of(
      // ── Large-Cap Blend / Index ──────────────────────────────────────────
      new FundOption("vanguard-500",  "Vanguard 500 Index Admiral",              "VFIAX"),
      new FundOption("fxaix",         "Fidelity 500 Index Fund",                 "FXAIX"),
      new FundOption("ivv",           "iShares Core S&P 500 ETF",                "IVV"),
      new FundOption("spy",           "SPDR S&P 500 ETF Trust",                  "SPY"),
      new FundOption("voog",          "Vanguard S&P 500 Growth ETF",             "VOOG"),

      // ── Large-Cap Growth ─────────────────────────────────────────────────
      new FundOption("fidelity-growth","Fidelity Growth Company",                "FDGRX"),
      new FundOption("trowe-bluechip", "T. Rowe Price Blue Chip Growth",         "TRBCX"),
      new FundOption("fcntx",          "Fidelity Contrafund",                    "FCNTX"),
      new FundOption("agthx",          "American Funds Growth Fund of America",  "AGTHX"),
      new FundOption("qqq",            "Invesco QQQ Trust (Nasdaq-100)",         "QQQ"),
      new FundOption("arkk",           "ARK Innovation ETF",                     "ARKK"),
      new FundOption("xlk",            "Technology Select Sector SPDR",          "XLK"),

      // ── Total Market ─────────────────────────────────────────────────────
      new FundOption("schwab-total",  "Schwab Total Market Index",               "SWTSX"),
      new FundOption("vti",           "Vanguard Total Stock Market ETF",         "VTI"),
      new FundOption("vtsax",         "Vanguard Total Stock Market Admiral",     "VTSAX"),

      // ── Balanced / Allocation ────────────────────────────────────────────
      new FundOption("vwelx",         "Vanguard Wellington Fund",                "VWELX"),
      new FundOption("prwcx",         "T. Rowe Price Capital Appreciation",      "PRWCX"),

      // ── International ────────────────────────────────────────────────────
      new FundOption("dodfx",         "Dodge & Cox International Stock",         "DODFX"),

      // ── Fixed Income / Bond ──────────────────────────────────────────────
      new FundOption("pimco-total",   "PIMCO Total Return",                      "PTTRX"),
      new FundOption("agg",           "iShares Core U.S. Aggregate Bond ETF",    "AGG"),
      new FundOption("bnd",           "Vanguard Total Bond Market ETF",          "BND")
  );

  private static final Map<String, String> FUND_TICKERS;
  private static final Map<String, String> TICKER_CANONICAL;

  static {
    Map<String, String> ft = new HashMap<>();
    Map<String, String> tc = new HashMap<>();
    for (FundOption f : FUNDS) {
      ft.put(f.fundId(), f.ticker());
      tc.put(f.ticker().toUpperCase(), f.ticker());
    }
    FUND_TICKERS = Collections.unmodifiableMap(ft);
    TICKER_CANONICAL = Collections.unmodifiableMap(tc);
  }

  public List<FundOption> listFunds() {
    return FUNDS;
  }

  public Optional<String> tickerFor(String fundId) {
    if (fundId == null || fundId.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(FUND_TICKERS.get(fundId));
  }

  public Optional<String> nameFor(String fundId) {
    return FUNDS.stream()
        .filter(f -> f.fundId().equals(fundId))
        .map(FundOption::name)
        .findFirst();
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
