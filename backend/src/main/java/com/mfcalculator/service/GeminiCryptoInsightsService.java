package com.mfcalculator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mfcalculator.dto.CryptoAiInsightsResponse;
import com.mfcalculator.dto.CryptoFeeAuditResponse;
import com.mfcalculator.dto.CryptoHoldingResponse;
import com.mfcalculator.dto.CryptoMarketPulseResponse;
import com.mfcalculator.dto.CryptoOnchainSummaryResponse;
import com.mfcalculator.dto.CryptoPerformanceAuditResponse;
import com.mfcalculator.dto.CryptoStressTestResponse;
import com.mfcalculator.dto.CryptoTaxOptimizerResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiCryptoInsightsService {
  private static final Logger logger = LoggerFactory.getLogger(GeminiCryptoInsightsService.class);

  private static final String INSIGHTS_PROMPT = """
      You are writing concise institutional crypto portfolio insights for a dashboard.
      Use only the supplied metrics. Do not invent new figures.
      Return JSON only.
      Requirements:
      - headline: 4-9 words
      - summary: 1-2 sentences
      - bullets: exactly 3 short actionable insights
      - outlook: one sentence describing the main forward-looking risk or opportunity
      - Make the narrative materially different depending on the portfolio archetype, dominant assets, stablecoin weight, winners/losers, and concentration profile.
      - Do not reuse generic phrases like "optimization opportunities across taxes, execution, and concentration risk" unless the metrics truly force that framing.
      - Lead with what makes this portfolio distinctive versus a typical crypto portfolio.
      - If the book is concentrated, say what it is concentrated in. If it is diversified, say that explicitly. If it has a large stablecoin reserve, discuss deployment optionality. If one major asset is deeply underwater, discuss that drawdown specifically.
      Keep the tone professional, sharp, and data-driven.
      """;

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final String model;
  private final String baseUrl;

  public GeminiCryptoInsightsService(
      RestTemplateBuilder restTemplateBuilder,
      ObjectMapper objectMapper,
      @Value("${gemini.apiKey:${GEMINI_API_KEY:}}") String apiKey,
      @Value("${gemini.model:${GEMINI_MODEL:gemini-2.5-flash}}") String model,
      @Value("${gemini.baseUrl:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
      @Value("${gemini.timeoutSeconds:20}") long timeoutSeconds
  ) {
    this.restTemplate = restTemplateBuilder
        .setConnectTimeout(Duration.ofSeconds(timeoutSeconds))
        .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
        .build();
    this.objectMapper = objectMapper;
    this.apiKey = apiKey;
    this.model = model;
    this.baseUrl = baseUrl;
  }

  public CryptoAiInsightsResponse buildInsights(
      List<CryptoHoldingResponse> holdings,
      CryptoTaxOptimizerResponse taxOptimizer,
      CryptoPerformanceAuditResponse performanceAudit,
      CryptoFeeAuditResponse feeAudit,
      CryptoStressTestResponse stressTest,
      CryptoMarketPulseResponse marketPulse,
      CryptoOnchainSummaryResponse onchainSummary
  ) {
    if (apiKey == null || apiKey.isBlank()) {
      return fallbackInsights(holdings, taxOptimizer, performanceAudit, feeAudit, marketPulse, onchainSummary);
    }

    try {
      String prompt = buildPrompt(holdings, taxOptimizer, performanceAudit, feeAudit, stressTest, marketPulse, onchainSummary);
      String content = requestInsights(prompt);
      return parseInsights(content);
    } catch (RestClientResponseException ex) {
      logger.warn("Gemini crypto insights failed: status={}, message={}", ex.getRawStatusCode(), ex.getMessage());
    } catch (RuntimeException ex) {
      logger.warn("Gemini crypto insights failed: message={}", ex.getMessage());
    }

    return fallbackInsights(holdings, taxOptimizer, performanceAudit, feeAudit, marketPulse, onchainSummary);
  }

  private String buildPrompt(
      List<CryptoHoldingResponse> holdings,
      CryptoTaxOptimizerResponse taxOptimizer,
      CryptoPerformanceAuditResponse performanceAudit,
      CryptoFeeAuditResponse feeAudit,
      CryptoStressTestResponse stressTest,
      CryptoMarketPulseResponse marketPulse,
      CryptoOnchainSummaryResponse onchainSummary
  ) {
    String topHolding = holdings.isEmpty()
        ? "None"
        : holdings.stream().max(java.util.Comparator.comparingDouble(CryptoHoldingResponse::currentValue))
            .map(h -> h.asset() + " at $" + h.currentValue())
            .orElse("None");
    double totalValue = holdings.stream().mapToDouble(CryptoHoldingResponse::currentValue).sum();
    double stablecoinWeight = totalValue <= 0 ? 0.0 : holdings.stream()
        .filter(h -> isStablecoin(h.asset()))
        .mapToDouble(CryptoHoldingResponse::currentValue)
        .sum() / totalValue * 100.0;
    double topHoldingWeight = totalValue <= 0 || holdings.isEmpty() ? 0.0 : holdings.stream()
        .mapToDouble(CryptoHoldingResponse::currentValue)
        .max()
        .orElse(0.0) / totalValue * 100.0;
    String bestPerformer = holdings.stream()
        .max(java.util.Comparator.comparingDouble(CryptoHoldingResponse::unrealizedPnLPct))
        .map(h -> h.asset() + " " + h.unrealizedPnLPct() + "%")
        .orElse("None");
    String worstPerformer = holdings.stream()
        .min(java.util.Comparator.comparingDouble(CryptoHoldingResponse::unrealizedPnLPct))
        .map(h -> h.asset() + " " + h.unrealizedPnLPct() + "%")
        .orElse("None");
    String archetype = describeArchetype(holdings, stablecoinWeight, topHoldingWeight);
    String concentration = topHoldingWeight >= 45.0 ? "high" : topHoldingWeight >= 30.0 ? "moderate" : "low";
    String topThree = holdings.stream()
        .sorted(java.util.Comparator.comparingDouble(CryptoHoldingResponse::currentValue).reversed())
        .limit(3)
        .map(h -> {
          double weight = totalValue <= 0 ? 0.0 : h.currentValue() / totalValue * 100.0;
          return h.asset() + " " + Math.round(weight) + "%";
        })
        .reduce((a, b) -> a + ", " + b)
        .orElse("None");

    return """
        Portfolio metrics:
        archetype=%s
        topHolding=%s
        topHoldingWeightPct=%s
        concentrationLevel=%s
        topThreeHoldings=%s
        stablecoinWeightPct=%s
        bestPerformer=%s
        worstPerformer=%s
        holdingCount=%s
        taxSavings=%s
        recommendedTaxMethod=%s
        realizedPnl=%s
        holdVsSellDelta=%s
        feeSavings=%s
        spreadBps=%s
        bookImbalancePct=%s
        stressMedian5y=%s
        stressDownside95=%s
        baseActivity=%s
        onchainTx30d=%s
        bridgeVolume=%s
        """.formatted(
        archetype,
        topHolding,
        round(topHoldingWeight),
        concentration,
        topThree,
        round(stablecoinWeight),
        bestPerformer,
        worstPerformer,
        holdings.size(),
        taxOptimizer.estimatedTaxSavings(),
        taxOptimizer.recommendedMethod(),
        performanceAudit.realizedPnL(),
        performanceAudit.holdVsSellDelta(),
        feeAudit.potentialSavings(),
        marketPulse.spreadBps(),
        marketPulse.imbalancePct(),
        stressTest.scenarios().isEmpty() ? 0 : stressTest.scenarios().get(0).fiveYearMedian(),
        stressTest.scenarios().isEmpty() ? 0 : stressTest.scenarios().get(0).downside95(),
        onchainSummary.baseActivityDetected(),
        onchainSummary.transactionCount30d(),
        onchainSummary.bridgeVolumeUsd30d()
    );
  }

  private String requestInsights(String prompt) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("x-goog-api-key", apiKey);

    Map<String, Object> payload = Map.of(
        "systemInstruction", Map.of("parts", List.of(Map.of("text", INSIGHTS_PROMPT))),
        "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
        "generationConfig", Map.of(
            "temperature", 0.4,
            "responseMimeType", "application/json",
            "responseJsonSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "headline", Map.of("type", "string"),
                    "summary", Map.of("type", "string"),
                    "bullets", Map.of("type", "array", "items", Map.of("type", "string")),
                    "outlook", Map.of("type", "string")
                ),
                "required", List.of("headline", "summary", "bullets", "outlook"),
                "propertyOrdering", List.of("headline", "summary", "bullets", "outlook")
            )
        )
    );

    ResponseEntity<Map> entity = restTemplate.exchange(
        baseUrl + "/models/" + model + ":generateContent",
        HttpMethod.POST,
        new HttpEntity<>(payload, headers),
        Map.class
    );
    return extractText(entity.getBody());
  }

  private String extractText(Map<?, ?> response) {
    if (response == null) {
      throw new IllegalStateException("Gemini response body was empty");
    }
    Object candidates = response.get("candidates");
    if (!(candidates instanceof List<?> candidateList) || candidateList.isEmpty()) {
      throw new IllegalStateException("Gemini response contained no candidates");
    }
    Object firstCandidate = candidateList.get(0);
    if (!(firstCandidate instanceof Map<?, ?> candidateMap)) {
      throw new IllegalStateException("Gemini candidate was not an object");
    }
    Object content = candidateMap.get("content");
    if (!(content instanceof Map<?, ?> contentMap)) {
      throw new IllegalStateException("Gemini response contained no content");
    }
    Object parts = contentMap.get("parts");
    if (!(parts instanceof List<?> partList) || partList.isEmpty()) {
      throw new IllegalStateException("Gemini response contained no parts");
    }
    Object firstPart = partList.get(0);
    if (!(firstPart instanceof Map<?, ?> partMap)) {
      throw new IllegalStateException("Gemini response part was not an object");
    }
    Object text = partMap.get("text");
    if (!(text instanceof String json) || json.isBlank()) {
      throw new IllegalStateException("Gemini response text was empty");
    }
    return json;
  }

  private CryptoAiInsightsResponse parseInsights(String content) {
    try {
      JsonNode root = objectMapper.readTree(content);
      List<String> bullets = root.path("bullets").isArray()
          ? java.util.stream.StreamSupport.stream(root.path("bullets").spliterator(), false)
              .map(JsonNode::asText)
              .filter(text -> !text.isBlank())
              .limit(3)
              .toList()
          : List.of();
      if (bullets.size() < 3) {
        throw new IllegalStateException("Gemini insights did not return 3 bullets");
      }
      return new CryptoAiInsightsResponse(
          root.path("headline").asText(),
          root.path("summary").asText(),
          bullets,
          root.path("outlook").asText()
      );
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to parse Gemini crypto insights JSON", ex);
    }
  }

  private CryptoAiInsightsResponse fallbackInsights(
      List<CryptoHoldingResponse> holdings,
      CryptoTaxOptimizerResponse taxOptimizer,
      CryptoPerformanceAuditResponse performanceAudit,
      CryptoFeeAuditResponse feeAudit,
      CryptoMarketPulseResponse marketPulse,
      CryptoOnchainSummaryResponse onchainSummary
  ) {
    double totalValue = holdings.stream().mapToDouble(CryptoHoldingResponse::currentValue).sum();
    double topWeight = totalValue <= 0 || holdings.isEmpty() ? 0.0 : holdings.stream()
        .mapToDouble(CryptoHoldingResponse::currentValue)
        .max()
        .orElse(0.0) / totalValue * 100.0;
    double stablecoinWeight = totalValue <= 0 ? 0.0 : holdings.stream()
        .filter(h -> isStablecoin(h.asset()))
        .mapToDouble(CryptoHoldingResponse::currentValue)
        .sum() / totalValue * 100.0;
    String archetype = describeArchetype(holdings, stablecoinWeight, topWeight);
    CryptoHoldingResponse leader = holdings.stream()
        .max(java.util.Comparator.comparingDouble(CryptoHoldingResponse::currentValue))
        .orElse(null);
    CryptoHoldingResponse laggard = holdings.stream()
        .min(java.util.Comparator.comparingDouble(CryptoHoldingResponse::unrealizedPnLPct))
        .orElse(null);

    if (stablecoinWeight >= 20.0) {
      String headline = leader != null && "BTC".equalsIgnoreCase(leader.asset())
          ? "Bitcoin Core, Cash Optionality"
          : "Cash-Heavy Tactical Book";
      return new CryptoAiInsightsResponse(
          headline,
          leader != null && "BTC".equalsIgnoreCase(leader.asset())
              ? "This portfolio is running a barbell structure: a dominant Bitcoin core with a meaningful stablecoin reserve on the sidelines. That makes it less of a fully risk-on crypto book and more of a tactical deployment book."
              : "This book is carrying a meaningful cash-like reserve rather than staying fully deployed into beta. The portfolio is positioned for optionality, not maximum directional exposure.",
          List.of(
              stablecoinWeight >= 30.0
                  ? "The stablecoin sleeve is large enough to act as a true deployment reserve rather than just residual cash."
                  : "Stablecoin exposure is high enough to matter tactically, which gives you room to redeploy into weakness without forced selling.",
              leader == null ? "Core exposure is still concentrated in a small number of assets." : leader.asset() + " still anchors portfolio direction despite the reserve capital.",
              taxOptimizer.recommendedMethod() + " remains the cleanest path for realizing losses or trimming gains with about $" + Math.round(taxOptimizer.estimatedTaxSavings()) + " in modeled tax value."
          ),
          leader != null && "BTC".equalsIgnoreCase(leader.asset())
              ? "The main choice ahead is whether to keep pairing Bitcoin with dry powder or rotate that cash buffer into higher-beta follow-through."
              : "The key decision is whether to keep preserving optionality in cash or rotate that reserve back into risk if crypto breadth improves."
      );
    }

    if (topWeight >= 45.0) {
      if (leader != null && "BTC".equalsIgnoreCase(leader.asset())) {
        if (laggard != null && "ETH".equalsIgnoreCase(laggard.asset()) && laggard.unrealizedPnLPct() < -20.0) {
          return new CryptoAiInsightsResponse(
              "Bitcoin Strength, Ethereum Drag",
              "This book behaves like a Bitcoin-led conviction portfolio with a meaningful Ethereum drag layered underneath it. The result is a portfolio where the core thesis is working, but the secondary beta sleeve is diluting efficiency.",
              List.of(
                  "BTC is doing the heavy lifting, so portfolio direction is still anchored to one macro-quality crypto asset.",
                  "ETH is the main performance leak in the book, which turns sizing and tax treatment there into a strategic decision rather than portfolio housekeeping.",
                  "Modeled fee savings of about $" + Math.round(feeAudit.potentialSavings()) + " matter, but they are secondary to the much larger BTC-versus-ETH capital allocation question."
              ),
              "Forward returns will depend less on broad crypto beta than on whether Ethereum starts catching up to the Bitcoin-led core."
          );
        }

        if (laggard != null && laggard.unrealizedPnLPct() < -20.0) {
          return new CryptoAiInsightsResponse(
              "Bitcoin Core, Alt Tail Risk",
              "This is a concentrated Bitcoin-first portfolio with a smaller altcoin tail that is not pulling its weight. The portfolio is not broadly diversified; it is a core BTC view with an attached idiosyncratic risk sleeve.",
              List.of(
                  "BTC remains the dominant engine, which means the portfolio still expresses a high-conviction core view rather than a balanced crypto basket.",
                  laggard.asset() + " is the weak tail exposure and is now a better candidate for a deliberate trim, harvest, or rotation than for passive holding.",
                  "Execution drag of roughly $" + Math.round(feeAudit.potentialSavings()) + " is worth addressing, but the sharper value decision is whether the alt sleeve still deserves capital."
              ),
              "The next move is deciding if this should stay a Bitcoin-plus-optional-alts book or become a cleaner Bitcoin core allocation."
          );
        }

        return new CryptoAiInsightsResponse(
            "Bitcoin Conviction Book",
            "This portfolio is not trying to mirror the crypto market evenly; it is expressing a high-conviction Bitcoin stance. That can be powerful when BTC leadership persists, but it leaves little diversification if leadership broadens elsewhere.",
            List.of(
                "BTC is the clear risk engine and will dictate most portfolio outcomes from here.",
                topWeight >= 55.0
                    ? "Concentration is high enough that secondary holdings act more like satellites than true diversifiers."
                    : "The supporting positions provide some breadth, but they do not meaningfully offset the dominant BTC exposure.",
                "Fee savings of about $" + Math.round(feeAudit.potentialSavings()) + " are still available, though concentration remains the more important portfolio characteristic."
            ),
            "The core risk is not crypto in general, but whether the portfolio remains too dependent on Bitcoin leadership alone."
        );
      }

      if (leader != null && isLayerOne(leader.asset())) {
        return new CryptoAiInsightsResponse(
            "High-Beta Leader Dominates",
            "This portfolio is concentrated in a high-beta layer-one winner rather than in a defensive core asset. That makes the upside path more explosive, but it also makes the drawdown profile less forgiving.",
            List.of(
                leader.asset() + " is carrying the book, so performance dispersion versus Bitcoin could become the defining driver of returns.",
                laggard != null && laggard.unrealizedPnLPct() < -20.0
                    ? laggard.asset() + " is weakening the portfolio's secondary sleeve and deserves an explicit risk-budget decision."
                    : "The secondary positions are too small to materially change the concentration story.",
                "Tax alpha of about $" + Math.round(taxOptimizer.estimatedTaxSavings()) + " is available, but the larger question is whether this level of beta concentration is still intentional."
            ),
            "If market leadership rotates away from high-beta layer ones, the portfolio could feel much more fragile than a broader crypto allocation."
        );
      }

      return new CryptoAiInsightsResponse(
          "Single-Asset Risk Leads",
          "This portfolio is being driven primarily by one oversized position rather than by broad crypto diversification. That concentration can create strong upside, but it also narrows the sources of performance.",
          List.of(
              leader == null ? "One holding dominates the book." : leader.asset() + " is the clear risk engine and will drive most of the portfolio's variance from here.",
              laggard != null && laggard.unrealizedPnLPct() < -20 ? laggard.asset() + " is materially underwater, which creates a more portfolio-specific tax and sizing decision." : "The secondary positions matter less than the lead asset in the current setup.",
              "Execution savings of roughly $" + Math.round(feeAudit.potentialSavings()) + " are still worth capturing, but concentration is the bigger strategic variable."
          ),
          "The main forward risk is that portfolio outcomes remain tightly tied to one coin rather than to the broader crypto market."
      );
    }

    if (laggard != null && laggard.unrealizedPnLPct() < -25.0) {
      String headline = "ETH".equalsIgnoreCase(laggard.asset())
          ? "Ethereum Recovery Decision"
          : "Drawdown Recovery Book";
      return new CryptoAiInsightsResponse(
          headline,
          "ETH".equalsIgnoreCase(laggard.asset())
              ? "This portfolio is carrying a significant Ethereum drawdown that changes the entire character of the book. The challenge here is less about finding more upside and more about deciding whether the underwater ETH sleeve still matches the portfolio's conviction."
              : "This portfolio is defined less by momentum leadership and more by a large underwater sleeve that is dragging on aggregate efficiency. The analytics suggest a recovery-and-repositioning problem rather than a pure beta problem.",
          List.of(
              laggard.asset() + " is the main drawdown pocket in the book and should be treated as an explicit capital-allocation decision, not passive baggage.",
              taxOptimizer.recommendedMethod() + " offers the strongest immediate cleanup path with about $" + Math.round(taxOptimizer.estimatedTaxSavings()) + " in modeled tax benefit.",
              performanceAudit.holdVsSellDelta() > 0 ? "Past timing decisions left money on the table versus a simple hold benchmark." : "Timing damage has been limited relative to the live drawdown in current holdings."
          ),
          "The best opportunity is to decide whether the lagging sleeve still deserves capital or should be harvested and rotated into stronger exposures."
      );
    }

    return new CryptoAiInsightsResponse(
        "Balanced Beta Book",
        "This portfolio is spread across multiple major crypto exposures rather than hinging on a single position or a large cash reserve. The key story here is portfolio construction discipline more than one-off hero trades.",
        List.of(
            leader == null ? "Performance is being shared across several holdings." : leader.asset() + " leads the book, but the portfolio is not exclusively dependent on it.",
            "Tax and execution efficiency still matter, with about $" + Math.round(taxOptimizer.estimatedTaxSavings()) + " in tax value and $" + Math.round(feeAudit.potentialSavings()) + " in fee savings available.",
            onchainSummary.baseActivityDetected() ? "Base activity adds a second layer of deployment behavior beyond exchange balances alone." : "The absence of on-chain activity keeps this portfolio closer to an exchange-held allocation book."
        ),
        "The next step is improving capital efficiency while preserving diversification rather than making a large directional bet."
    );
  }

  private String describeArchetype(List<CryptoHoldingResponse> holdings, double stablecoinWeight, double topHoldingWeight) {
    if (holdings.isEmpty()) {
      return "empty";
    }
    if (stablecoinWeight >= 20.0) {
      return "cash-heavy tactical book";
    }
    if (topHoldingWeight >= 45.0) {
      return "high-conviction concentrated book";
    }
    boolean hasBtc = holdings.stream().anyMatch(h -> "BTC".equalsIgnoreCase(h.asset()));
    boolean hasEth = holdings.stream().anyMatch(h -> "ETH".equalsIgnoreCase(h.asset()));
    boolean hasSol = holdings.stream().anyMatch(h -> "SOL".equalsIgnoreCase(h.asset()));
    if (hasBtc && hasEth && hasSol) {
      return "multi-asset core crypto book";
    }
    return "idiosyncratic alt-tilted portfolio";
  }

  private boolean isStablecoin(String asset) {
    String symbol = asset == null ? "" : asset.toUpperCase();
    return symbol.equals("USDC") || symbol.equals("USDT") || symbol.equals("DAI") || symbol.equals("FDUSD");
  }

  private boolean isLayerOne(String asset) {
    String symbol = asset == null ? "" : asset.toUpperCase();
    return symbol.equals("ETH") || symbol.equals("SOL") || symbol.equals("ADA") || symbol.equals("AVAX") || symbol.equals("MATIC");
  }

  private double round(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
