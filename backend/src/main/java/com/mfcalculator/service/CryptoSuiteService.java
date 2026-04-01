package com.mfcalculator.service;

import com.mfcalculator.dto.CryptoAiInsightsResponse;
import com.mfcalculator.dto.CryptoFanPointResponse;
import com.mfcalculator.dto.CryptoFeeAuditResponse;
import com.mfcalculator.dto.CryptoHoldingResponse;
import com.mfcalculator.dto.CryptoLotRecommendationResponse;
import com.mfcalculator.dto.CryptoMarketPulseResponse;
import com.mfcalculator.dto.CryptoOnchainSummaryResponse;
import com.mfcalculator.dto.CryptoPerformanceAuditResponse;
import com.mfcalculator.dto.CryptoScenarioResponse;
import com.mfcalculator.dto.CryptoStressTestResponse;
import com.mfcalculator.dto.CryptoSuiteResponse;
import com.mfcalculator.dto.CryptoTaxOptimizerResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CryptoSuiteService {
  private final CoinbaseCryptoDataClient coinbaseCryptoDataClient;
  private final GeminiCryptoInsightsService geminiCryptoInsightsService;

  public CryptoSuiteService(
      CoinbaseCryptoDataClient coinbaseCryptoDataClient,
      GeminiCryptoInsightsService geminiCryptoInsightsService
  ) {
    this.coinbaseCryptoDataClient = coinbaseCryptoDataClient;
    this.geminiCryptoInsightsService = geminiCryptoInsightsService;
  }

  public CryptoSuiteResponse buildSuite() {
    return buildSuite(null);
  }

  public CryptoSuiteResponse buildSuite(Long userId) {
    CryptoDataSnapshot snapshot = coinbaseCryptoDataClient.fetchSnapshot(userId);

    List<Lot> openLots = openLots(snapshot.transactions());
    List<CryptoHoldingResponse> holdings = holdings(snapshot, openLots);
    CryptoTaxOptimizerResponse taxOptimizer = taxOptimizer(snapshot, openLots);
    CryptoPerformanceAuditResponse performanceAudit = performanceAudit(snapshot);
    CryptoFeeAuditResponse feeAudit = feeAudit(snapshot);
    CryptoStressTestResponse stressTest = stressTest(holdings);
    CryptoMarketPulseResponse marketPulse = marketPulse(snapshot.marketSnapshot());
    CryptoOnchainSummaryResponse onchainSummary = onchainSummary(snapshot.onchainSnapshot());
    CryptoAiInsightsResponse aiInsights = holdings.isEmpty()
        ? null
        : geminiCryptoInsightsService.buildInsights(
            holdings,
            taxOptimizer,
            performanceAudit,
            feeAudit,
            stressTest,
            marketPulse,
            onchainSummary
        );

    return new CryptoSuiteResponse(
        snapshot.connected(),
        snapshot.demoMode(),
        "Coinbase",
        snapshot.connected()
            ? "Coinbase Connected"
            : snapshot.uploadedWallet()
            ? "Uploaded Wallet Active"
            : "Wallet Upload Required",
        snapshot.connected()
            ? "Live balances, transactions, and Coinbase market pricing are powering this suite."
            : snapshot.uploadedWallet()
            ? "Your uploaded wallet file is now powering the crypto suite, with Coinbase market overlays layered on top."
            : "Upload a wallet export in JSON or CSV to populate the crypto suite with your actual holdings and analytics.",
        snapshot.connectUrl(),
        snapshot.sourceLabel(),
        Instant.now().toString(),
        aiInsights,
        holdings,
        taxOptimizer,
        performanceAudit,
        feeAudit,
        stressTest,
        marketPulse,
        onchainSummary
    );
  }

  private List<Lot> openLots(List<CryptoTransaction> transactions) {
    Map<String, List<Lot>> lotsByAsset = new LinkedHashMap<>();

    for (CryptoTransaction transaction : transactions.stream()
        .sorted(Comparator.comparing(CryptoTransaction::timestamp))
        .toList()) {
      lotsByAsset.computeIfAbsent(transaction.asset(), ignored -> new ArrayList<>());
      if (transaction.type() == CryptoTransactionType.BUY) {
        lotsByAsset.get(transaction.asset()).add(new Lot(
            transaction.asset(),
            transaction.assetName(),
            transaction.timestamp().toLocalDate(),
            transaction.quantity(),
            transaction.pricePerUnitUsd(),
            transaction.pricePerUnitUsd() * transaction.quantity()
        ));
      } else if (transaction.type() == CryptoTransactionType.SELL) {
        double remaining = transaction.quantity();
        List<Lot> lots = lotsByAsset.get(transaction.asset());
        for (Lot lot : lots) {
          if (remaining <= 0) {
            break;
          }
          double consumed = Math.min(lot.remainingQuantity(), remaining);
          lot.consume(consumed);
          remaining -= consumed;
        }
      }
    }

    return lotsByAsset.values().stream()
        .flatMap(List::stream)
        .filter(lot -> lot.remainingQuantity() > 0.0000001)
        .toList();
  }

  private List<CryptoHoldingResponse> holdings(CryptoDataSnapshot snapshot, List<Lot> openLots) {
    Map<String, Double> costBasisByAsset = new LinkedHashMap<>();
    openLots.forEach(lot -> costBasisByAsset.merge(
        lot.asset(),
        lot.remainingQuantity() * lot.unitCostUsd(),
        Double::sum
    ));

    return snapshot.balances().stream()
        .filter(balance -> balance.quantity() > 0.0000001)
        .map(balance -> {
          double costBasis = costBasisByAsset.getOrDefault(balance.asset(), 0.0);
          double currentValue = balance.quantity() * balance.currentPriceUsd();
          double pnl = currentValue - costBasis;
          double pnlPct = costBasis <= 0 ? 0 : pnl / costBasis * 100.0;
          return new CryptoHoldingResponse(
              balance.asset(),
              balance.assetName(),
              balance.quantity(),
              round(costBasis),
              round(balance.currentPriceUsd()),
              round(currentValue),
              round(pnl),
              round(pnlPct)
          );
        })
        .sorted(Comparator.comparing(CryptoHoldingResponse::currentValue).reversed())
        .toList();
  }

  private CryptoTaxOptimizerResponse taxOptimizer(CryptoDataSnapshot snapshot, List<Lot> openLots) {
    double currentTaxRate = 0.37;
    List<Lot> ranked = openLots.stream()
        .sorted(Comparator.comparing(Lot::unitCostUsd).reversed())
        .limit(4)
        .toList();

    double fifoGain = realizedGainUsing(openLots, snapshot.pricesByAsset(), false);
    double hifoGain = realizedGainUsing(openLots, snapshot.pricesByAsset(), true);
    double estimatedSavings = Math.max(0, (fifoGain - hifoGain) * currentTaxRate);

    List<CryptoLotRecommendationResponse> recommendations = ranked.stream()
        .map(lot -> {
          double currentPrice = snapshot.pricesByAsset().getOrDefault(lot.asset(), lot.unitCostUsd());
          double gain = (currentPrice - lot.unitCostUsd()) * lot.remainingQuantity();
          double taxImpact = gain * currentTaxRate;
          return new CryptoLotRecommendationResponse(
              lot.asset(),
              lot.acquiredAt().toString(),
              round(lot.remainingQuantity()),
              round(lot.unitCostUsd()),
              round(currentPrice),
              round(gain),
              round(taxImpact),
              gain < 0
                  ? "Harvesting this lot realizes a loss first and offsets short-term gains."
                  : "This high-cost lot minimizes gains versus lower-cost inventory under a HIFO sale."
          );
        })
        .toList();

    String recommendedMethod = hifoGain <= fifoGain ? "HIFO" : "FIFO";
    String summary = recommendedMethod.equals("HIFO")
        ? "Selling highest-cost lots first reduces taxable gains on the current book."
        : "FIFO is already as efficient as HIFO on the current inventory mix.";

    return new CryptoTaxOptimizerResponse(
        round(estimatedSavings),
        round(fifoGain),
        round(hifoGain),
        recommendedMethod,
        summary,
        recommendations
    );
  }

  private double realizedGainUsing(List<Lot> openLots, Map<String, Double> pricesByAsset, boolean hifo) {
    Map<String, List<Lot>> grouped = openLots.stream()
        .collect(Collectors.groupingBy(Lot::asset));

    double total = 0.0;
    for (List<Lot> lots : grouped.values()) {
      List<Lot> ranked = new ArrayList<>(lots);
      ranked.sort(hifo
          ? Comparator.comparing(Lot::unitCostUsd).reversed()
          : Comparator.comparing(Lot::acquiredAt));
      double qty = Math.min(0.25, ranked.stream().mapToDouble(Lot::remainingQuantity).sum() * 0.15);
      double currentPrice = pricesByAsset.getOrDefault(ranked.get(0).asset(), ranked.get(0).unitCostUsd());
      for (Lot lot : ranked) {
        if (qty <= 0) {
          break;
        }
        double sold = Math.min(qty, lot.remainingQuantity());
        total += (currentPrice - lot.unitCostUsd()) * sold;
        qty -= sold;
      }
    }
    return total;
  }

  private CryptoPerformanceAuditResponse performanceAudit(CryptoDataSnapshot snapshot) {
    double realizedPnL = 0;
    double holdVsSellDelta = 0;
    double missedPeakUpside = 0;
    for (CryptoTransaction transaction : snapshot.transactions()) {
      if (transaction.type() != CryptoTransactionType.SELL) {
        continue;
      }
      double proceeds = transaction.quantity() * transaction.pricePerUnitUsd();
      double currentValue = transaction.quantity() * snapshot.pricesByAsset().getOrDefault(transaction.asset(), transaction.pricePerUnitUsd());
      double peakValue = transaction.quantity() * transaction.peakPriceUsd();
      realizedPnL += proceeds - transaction.costBasisUsd();
      holdVsSellDelta += currentValue - proceeds;
      missedPeakUpside += Math.max(0, peakValue - proceeds);
    }

    double score = realizedPnL - Math.max(0, holdVsSellDelta * 0.35);
    List<String> highlights = List.of(
        holdVsSellDelta >= 0
            ? "Your sold positions would be worth more today than the proceeds you realized."
            : "Your exit timing has outperformed a simple hold-to-today benchmark on sold positions.",
        "Peak-price hindsight shows where profit-taking protected capital versus where upside was left on the table."
    );

    return new CryptoPerformanceAuditResponse(
        round(realizedPnL),
        round(holdVsSellDelta),
        round(missedPeakUpside),
        round(score),
        score >= 0 ? "Net positive timing edge" : "Hold strategy beat active timing",
        highlights
    );
  }

  private CryptoFeeAuditResponse feeAudit(CryptoDataSnapshot snapshot) {
    double fees = snapshot.transactions().stream().mapToDouble(CryptoTransaction::feeUsd).sum();
    double spreads = snapshot.transactions().stream().mapToDouble(tx ->
        Math.max(0, (tx.executedSpreadPct() - 0.0025) * tx.grossUsd())
    ).sum();
    double advancedTrade = snapshot.transactions().stream()
        .mapToDouble(tx -> tx.grossUsd() * 0.004)
        .sum();
    double notional = snapshot.transactions().stream().mapToDouble(CryptoTransaction::grossUsd).sum();
    double savings = Math.max(0, fees + spreads - advancedTrade);
    double effectiveRate = notional <= 0 ? 0 : (fees + spreads) / notional * 100.0;

    return new CryptoFeeAuditResponse(
        round(fees),
        round(spreads),
        round(advancedTrade),
        round(savings),
        round(effectiveRate),
        List.of(
            "Coinbase simple-trade style execution costs are benchmarked against a lower-fee Advanced Trade workflow.",
            "Spread drag is estimated from the difference between execution price and current Coinbase spot pricing."
        )
    );
  }

  private CryptoStressTestResponse stressTest(List<CryptoHoldingResponse> holdings) {
    double currentValue = holdings.stream().mapToDouble(CryptoHoldingResponse::currentValue).sum();
    double weightedVol = 0;
    for (CryptoHoldingResponse holding : holdings) {
      double weight = currentValue <= 0 ? 0 : holding.currentValue() / currentValue;
      weightedVol += weight * annualVolatility(holding.asset());
    }
    weightedVol = weightedVol == 0 ? 0.72 : weightedVol;

    List<CryptoScenarioResponse> scenarios = List.of(
        scenario("Bull", currentValue, 0.42, weightedVol * 0.85),
        scenario("Sideways", currentValue, 0.08, weightedVol * 0.7),
        scenario("Bear", currentValue, -0.18, weightedVol * 1.05),
        scenario("2018-Style Crash", currentValue, -0.35, weightedVol * 1.35)
    );

    List<CryptoFanPointResponse> fanChart = fanChart(currentValue, 0.16, weightedVol);
    return new CryptoStressTestResponse(round(currentValue), scenarios, fanChart);
  }

  private CryptoMarketPulseResponse marketPulse(CryptoMarketSnapshot snapshot) {
    List<String> highlights = List.of(
        snapshot.imbalancePct() >= 0
            ? "Bid depth currently outweighs ask depth, suggesting buyers are more aggressive near the mid."
            : "Ask depth is heavier than bids, indicating near-term sell-side pressure in the book.",
        "Spread and depth are framed in Advanced Trade-style market microstructure terms so users can compare execution quality against simple trade pricing."
    );
    return new CryptoMarketPulseResponse(
        snapshot.primaryPair(),
        round(snapshot.midPrice()),
        round(snapshot.spreadBps()),
        round(snapshot.bidDepthUsd()),
        round(snapshot.askDepthUsd()),
        round(snapshot.imbalancePct()),
        highlights
    );
  }

  private CryptoOnchainSummaryResponse onchainSummary(CryptoOnchainSnapshot snapshot) {
    List<String> highlights = snapshot.baseActivityDetected()
        ? List.of(
            "Base-related activity is present, so the suite is surfacing a 30-day view of wallet activity, gas spend, and bridge flow.",
            "This creates a unified picture across exchange balances and on-chain behavior rather than treating them as separate books."
        )
        : List.of(
            "No Base-style wallet activity was detected in the current dataset, so the on-chain panel is standing by for wallet upload or connected activity.",
            "Once wallet-level data is present, this panel can highlight bridge usage, gas drag, and recent on-chain turnover."
        );
    return new CryptoOnchainSummaryResponse(
        snapshot.baseActivityDetected(),
        snapshot.walletCount(),
        snapshot.transactionCount30d(),
        round(snapshot.gasSpentUsd30d()),
        round(snapshot.bridgeVolumeUsd30d()),
        highlights
    );
  }

  private CryptoScenarioResponse scenario(String name, double initialValue, double drift, double vol) {
    List<Double> ending = simulate(initialValue, drift, vol, 5, 400);
    List<Double> oneYear = simulate(initialValue, drift, vol, 1, 400);
    List<Double> threeYear = simulate(initialValue, drift, vol, 3, 400);
    return new CryptoScenarioResponse(
        name,
        round(percentile(oneYear, 50)),
        round(percentile(threeYear, 50)),
        round(percentile(ending, 50)),
        round(percentile(ending, 5)),
        round(percentile(ending, 95))
    );
  }

  private List<CryptoFanPointResponse> fanChart(double initialValue, double drift, double vol) {
    List<CryptoFanPointResponse> points = new ArrayList<>();
    for (int year = 1; year <= 5; year++) {
      List<Double> paths = simulate(initialValue, drift, vol, year, 500);
      points.add(new CryptoFanPointResponse(
          year,
          round(percentile(paths, 5)),
          round(percentile(paths, 25)),
          round(percentile(paths, 50)),
          round(percentile(paths, 75)),
          round(percentile(paths, 95))
      ));
    }
    return points;
  }

  private List<Double> simulate(double initialValue, double drift, double vol, int years, int simulations) {
    Random random = new Random(42L + years + simulations);
    List<Double> values = new ArrayList<>(simulations);
    for (int i = 0; i < simulations; i++) {
      double value = initialValue;
      for (int year = 0; year < years; year++) {
        double shock = random.nextGaussian();
        value *= Math.exp((drift - 0.5 * vol * vol) + vol * shock);
      }
      values.add(value);
    }
    values.sort(Double::compareTo);
    return values;
  }

  private double annualVolatility(String asset) {
    return switch (asset.toUpperCase(Locale.ROOT)) {
      case "BTC" -> 0.58;
      case "ETH" -> 0.72;
      case "SOL" -> 0.95;
      case "ADA" -> 0.88;
      default -> 0.80;
    };
  }

  private double percentile(List<Double> sortedValues, int percentile) {
    if (sortedValues.isEmpty()) {
      return 0;
    }
    int index = (int) Math.round((percentile / 100.0) * (sortedValues.size() - 1));
    return sortedValues.get(Math.max(0, Math.min(sortedValues.size() - 1, index)));
  }

  private double round(double value) {
    return Math.round(value * 100.0) / 100.0;
  }

  static final class CryptoDataSnapshot {
    private final boolean connected;
    private final boolean demoMode;
    private final boolean uploadedWallet;
    private final String connectUrl;
    private final List<CryptoBalance> balances;
    private final List<CryptoTransaction> transactions;
    private final Map<String, Double> pricesByAsset;
    private final CryptoMarketSnapshot marketSnapshot;
    private final CryptoOnchainSnapshot onchainSnapshot;
    private final String sourceLabel;

    CryptoDataSnapshot(
        boolean connected,
        boolean demoMode,
        boolean uploadedWallet,
        String connectUrl,
        List<CryptoBalance> balances,
        List<CryptoTransaction> transactions,
        Map<String, Double> pricesByAsset,
        CryptoMarketSnapshot marketSnapshot,
        CryptoOnchainSnapshot onchainSnapshot,
        String sourceLabel
    ) {
      this.connected = connected;
      this.demoMode = demoMode;
      this.uploadedWallet = uploadedWallet;
      this.connectUrl = connectUrl;
      this.balances = balances;
      this.transactions = transactions;
      this.pricesByAsset = pricesByAsset;
      this.marketSnapshot = marketSnapshot;
      this.onchainSnapshot = onchainSnapshot;
      this.sourceLabel = sourceLabel;
    }

    boolean connected() { return connected; }
    boolean demoMode() { return demoMode; }
    boolean uploadedWallet() { return uploadedWallet; }
    String connectUrl() { return connectUrl; }
    List<CryptoBalance> balances() { return balances; }
    List<CryptoTransaction> transactions() { return transactions; }
    Map<String, Double> pricesByAsset() { return pricesByAsset; }
    CryptoMarketSnapshot marketSnapshot() { return marketSnapshot; }
    CryptoOnchainSnapshot onchainSnapshot() { return onchainSnapshot; }
    String sourceLabel() { return sourceLabel; }
  }

  static final class CryptoBalance {
    private final String asset;
    private final String assetName;
    private final double quantity;
    private final double currentPriceUsd;

    CryptoBalance(String asset, String assetName, double quantity, double currentPriceUsd) {
      this.asset = asset;
      this.assetName = assetName;
      this.quantity = quantity;
      this.currentPriceUsd = currentPriceUsd;
    }

    String asset() { return asset; }
    String assetName() { return assetName; }
    double quantity() { return quantity; }
    double currentPriceUsd() { return currentPriceUsd; }
  }

  static final class CryptoMarketSnapshot {
    private final String primaryPair;
    private final double midPrice;
    private final double spreadBps;
    private final double bidDepthUsd;
    private final double askDepthUsd;
    private final double imbalancePct;

    CryptoMarketSnapshot(
        String primaryPair,
        double midPrice,
        double spreadBps,
        double bidDepthUsd,
        double askDepthUsd,
        double imbalancePct
    ) {
      this.primaryPair = primaryPair;
      this.midPrice = midPrice;
      this.spreadBps = spreadBps;
      this.bidDepthUsd = bidDepthUsd;
      this.askDepthUsd = askDepthUsd;
      this.imbalancePct = imbalancePct;
    }

    String primaryPair() { return primaryPair; }
    double midPrice() { return midPrice; }
    double spreadBps() { return spreadBps; }
    double bidDepthUsd() { return bidDepthUsd; }
    double askDepthUsd() { return askDepthUsd; }
    double imbalancePct() { return imbalancePct; }
  }

  static final class CryptoOnchainSnapshot {
    private final boolean baseActivityDetected;
    private final int walletCount;
    private final int transactionCount30d;
    private final double gasSpentUsd30d;
    private final double bridgeVolumeUsd30d;

    CryptoOnchainSnapshot(
        boolean baseActivityDetected,
        int walletCount,
        int transactionCount30d,
        double gasSpentUsd30d,
        double bridgeVolumeUsd30d
    ) {
      this.baseActivityDetected = baseActivityDetected;
      this.walletCount = walletCount;
      this.transactionCount30d = transactionCount30d;
      this.gasSpentUsd30d = gasSpentUsd30d;
      this.bridgeVolumeUsd30d = bridgeVolumeUsd30d;
    }

    boolean baseActivityDetected() { return baseActivityDetected; }
    int walletCount() { return walletCount; }
    int transactionCount30d() { return transactionCount30d; }
    double gasSpentUsd30d() { return gasSpentUsd30d; }
    double bridgeVolumeUsd30d() { return bridgeVolumeUsd30d; }
  }

  enum CryptoTransactionType { BUY, SELL }

  static final class CryptoTransaction {
    private final String asset;
    private final String assetName;
    private final CryptoTransactionType type;
    private final java.time.OffsetDateTime timestamp;
    private final double quantity;
    private final double pricePerUnitUsd;
    private final double feeUsd;
    private final double costBasisUsd;
    private final double peakPriceUsd;
    private final double executedSpreadPct;

    CryptoTransaction(
        String asset,
        String assetName,
        CryptoTransactionType type,
        java.time.OffsetDateTime timestamp,
        double quantity,
        double pricePerUnitUsd,
        double feeUsd,
        double costBasisUsd,
        double peakPriceUsd,
        double executedSpreadPct
    ) {
      this.asset = asset;
      this.assetName = assetName;
      this.type = type;
      this.timestamp = timestamp;
      this.quantity = quantity;
      this.pricePerUnitUsd = pricePerUnitUsd;
      this.feeUsd = feeUsd;
      this.costBasisUsd = costBasisUsd;
      this.peakPriceUsd = peakPriceUsd;
      this.executedSpreadPct = executedSpreadPct;
    }

    String asset() { return asset; }
    String assetName() { return assetName; }
    CryptoTransactionType type() { return type; }
    java.time.OffsetDateTime timestamp() { return timestamp; }
    double quantity() { return quantity; }
    double pricePerUnitUsd() { return pricePerUnitUsd; }
    double feeUsd() { return feeUsd; }
    double costBasisUsd() { return costBasisUsd; }
    double peakPriceUsd() { return peakPriceUsd; }
    double executedSpreadPct() { return executedSpreadPct; }
    double grossUsd() { return quantity * pricePerUnitUsd; }
  }

  static final class Lot {
    private final String asset;
    private final String assetName;
    private final LocalDate acquiredAt;
    private final double originalQuantity;
    private final double unitCostUsd;
    private double consumedQuantity;

    Lot(String asset, String assetName, LocalDate acquiredAt, double originalQuantity, double unitCostUsd, double ignored) {
      this.asset = asset;
      this.assetName = assetName;
      this.acquiredAt = acquiredAt;
      this.originalQuantity = originalQuantity;
      this.unitCostUsd = unitCostUsd;
    }

    void consume(double quantity) {
      consumedQuantity += quantity;
    }

    String asset() { return asset; }
    String assetName() { return assetName; }
    LocalDate acquiredAt() { return acquiredAt; }
    double unitCostUsd() { return unitCostUsd; }
    double remainingQuantity() { return Math.max(0, originalQuantity - consumedQuantity); }
  }
}
