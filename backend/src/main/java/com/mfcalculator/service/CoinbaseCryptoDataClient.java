package com.mfcalculator.service;

import static com.mfcalculator.service.CryptoSuiteService.CryptoBalance;
import static com.mfcalculator.service.CryptoSuiteService.CryptoDataSnapshot;
import static com.mfcalculator.service.CryptoSuiteService.CryptoMarketSnapshot;
import static com.mfcalculator.service.CryptoSuiteService.CryptoOnchainSnapshot;
import static com.mfcalculator.service.CryptoSuiteService.CryptoTransaction;
import static com.mfcalculator.service.CryptoSuiteService.CryptoTransactionType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mfcalculator.model.CryptoWalletUpload;
import com.mfcalculator.repository.CryptoWalletUploadRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class CoinbaseCryptoDataClient {
  private static final Logger logger = LoggerFactory.getLogger(CoinbaseCryptoDataClient.class);

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final CryptoWalletUploadRepository uploadRepository;
  private final String apiBaseUrl;
  private final String accessToken;
  private final String oauthClientId;
  private final String oauthRedirectUri;

  public CoinbaseCryptoDataClient(
      RestTemplateBuilder restTemplateBuilder,
      ObjectMapper objectMapper,
      CryptoWalletUploadRepository uploadRepository,
      @Value("${coinbase.apiBaseUrl:https://api.coinbase.com}") String apiBaseUrl,
      @Value("${coinbase.accessToken:${COINBASE_ACCESS_TOKEN:}}") String accessToken,
      @Value("${coinbase.oauthClientId:${COINBASE_OAUTH_CLIENT_ID:}}") String oauthClientId,
      @Value("${coinbase.oauthRedirectUri:${COINBASE_OAUTH_REDIRECT_URI:http://localhost:5173/crypto}}") String oauthRedirectUri,
      @Value("${coinbase.timeoutSeconds:20}") long timeoutSeconds
  ) {
    this.restTemplate = restTemplateBuilder
        .setConnectTimeout(Duration.ofSeconds(timeoutSeconds))
        .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
        .build();
    this.objectMapper = objectMapper;
    this.uploadRepository = uploadRepository;
    this.apiBaseUrl = apiBaseUrl;
    this.accessToken = accessToken;
    this.oauthClientId = oauthClientId;
    this.oauthRedirectUri = oauthRedirectUri;
  }

  public UploadSummary saveWalletUpload(Long userId, MultipartFile file) {
    try {
      ParsedWallet parsed = parseUpload(file);
      CryptoWalletUpload upload = new CryptoWalletUpload();
      upload.setUserId(userId);
      upload.setFileName(file.getOriginalFilename() == null ? "wallet-upload" : file.getOriginalFilename());
      upload.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
      upload.setPayloadJson(parsed.payloadJson());
      upload.setUploadedAt(Instant.now());
      uploadRepository.save(upload);
      return new UploadSummary(upload.getFileName(), parsed.holdings().size(), parsed.transactions().size(), upload.getUploadedAt());
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wallet upload format was not recognized");
    }
  }

  public CryptoDataSnapshot fetchSnapshot(Long userId) {
    Optional<CryptoWalletUpload> uploaded = userId == null
        ? Optional.empty()
        : uploadRepository.findTopByUserIdOrderByUploadedAtDesc(userId);
    if (uploaded.isPresent()) {
      return snapshotFromUpload(uploaded.get());
    }
    return emptySnapshot();
  }

  private CryptoDataSnapshot snapshotFromUpload(CryptoWalletUpload upload) {
    try {
      JsonNode root = objectMapper.readTree(upload.getPayloadJson());
      Map<String, Double> prices = new LinkedHashMap<>();
      List<CryptoBalance> balances = new ArrayList<>();
      List<CryptoTransaction> transactions = new ArrayList<>();

      for (JsonNode holding : root.path("holdings")) {
        String asset = holding.path("asset").asText();
        double price = holding.path("currentPriceUsd").asDouble();
        prices.put(asset, price);
        balances.add(new CryptoBalance(
            asset,
            holding.path("assetName").asText(asset),
            holding.path("quantity").asDouble(),
            price
        ));
      }

      for (JsonNode tx : root.path("transactions")) {
        transactions.add(new CryptoTransaction(
            tx.path("asset").asText(),
            tx.path("assetName").asText(tx.path("asset").asText()),
            CryptoTransactionType.valueOf(tx.path("type").asText("BUY")),
            OffsetDateTime.parse(tx.path("timestamp").asText()),
            tx.path("quantity").asDouble(),
            tx.path("pricePerUnitUsd").asDouble(),
            tx.path("feeUsd").asDouble(),
            tx.path("costBasisUsd").asDouble(),
            tx.path("peakPriceUsd").asDouble(),
            tx.path("executedSpreadPct").asDouble()
        ));
      }

      return new CryptoDataSnapshot(
          false,
          false,
          true,
          connectUrl(),
          balances,
          transactions,
          prices,
          fetchMarketSnapshot(prices),
          inferOnchainSnapshot(transactions, balances),
          "Uploaded wallet file"
      );
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to parse stored wallet upload", ex);
    }
  }

  private ParsedWallet parseUpload(MultipartFile file) {
    String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
    try {
      String text = new String(file.getBytes(), StandardCharsets.UTF_8);
      if (name.endsWith(".json")) {
        return parseJsonUpload(text);
      }
      return parseCsvUpload(text);
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read uploaded wallet file");
    }
  }

  private ParsedWallet parseJsonUpload(String text) throws IOException {
    JsonNode root = objectMapper.readTree(text);
    JsonNode holdingsNode = root.isArray() ? root : root.path("holdings");
    List<Map<String, Object>> holdings = new ArrayList<>();
    List<Map<String, Object>> transactions = new ArrayList<>();

    if (holdingsNode.isArray()) {
      for (JsonNode item : holdingsNode) {
        String asset = textOf(item, "asset", "symbol", "currency");
        if (asset == null) {
          continue;
        }
        double quantity = doubleOf(item, "quantity", "balance", "amount");
        double currentPrice = doubleOf(item, "currentPriceUsd", "currentPriceUSD", "currentPrice", "price", "spotPrice");
        double avgCost = doubleOf(item, "avgCostUsd", "avgCostBasisUSD", "avgCost", "averageCost", "costBasisPerUnit");
        if (currentPrice == 0.0 && avgCost > 0) {
          currentPrice = avgCost;
        }
        holdings.add(Map.of(
            "asset", asset.toUpperCase(),
            "assetName", textOf(item, "assetName", "name") == null ? asset.toUpperCase() : textOf(item, "assetName", "name"),
            "quantity", quantity,
            "currentPriceUsd", currentPrice
        ));
        JsonNode lotsNode = item.path("lots");
        if (lotsNode.isArray() && !lotsNode.isEmpty()) {
          for (JsonNode lot : lotsNode) {
            double lotQuantity = doubleOf(lot, "quantity", "amount");
            double lotUnitCost = doubleOf(lot, "costBasisPerUnit", "costBasisPerUnitUSD", "unitCostUsd");
            double lotTotal = doubleOf(lot, "costBasisTotal", "costBasisTotalUSD", "costBasisTotalUsd");
            if (lotUnitCost == 0.0 && lotQuantity > 0 && lotTotal > 0) {
              lotUnitCost = lotTotal / lotQuantity;
            }
            if (lotQuantity > 0 && lotUnitCost > 0) {
              transactions.add(Map.of(
                  "asset", asset.toUpperCase(),
                  "assetName", textOf(item, "assetName", "name") == null ? asset.toUpperCase() : textOf(item, "assetName", "name"),
                  "type", "BUY",
                  "timestamp", normalizeTimestamp(textOf(lot, "purchaseDate", "acquiredAt", "timestamp")),
                  "quantity", lotQuantity,
                  "pricePerUnitUsd", lotUnitCost,
                  "feeUsd", lotQuantity * lotUnitCost * 0.006,
                  "costBasisUsd", lotQuantity * lotUnitCost,
                  "peakPriceUsd", Math.max(currentPrice, lotUnitCost) * 1.15,
                  "executedSpreadPct", 0.01
              ));
            }
          }
        } else if (quantity > 0 && avgCost > 0) {
          transactions.add(defaultBuy(asset, quantity, avgCost, currentPrice));
        }
      }
    }

    JsonNode txNode = root.path("transactions");
    if (txNode.isArray()) {
      transactions.clear();
      for (JsonNode tx : txNode) {
        String asset = textOf(tx, "asset", "symbol", "currency");
        if (asset == null) {
          continue;
        }
        double quantity = doubleOf(tx, "quantity", "amount");
        double price = doubleOf(tx, "pricePerUnitUsd", "pricePerUnitUSD", "price", "unitPrice");
        double fee = doubleOf(tx, "feeUsd", "fee");
        String type = textOf(tx, "type", "side");
        String timestamp = textOf(tx, "timestamp", "date", "createdAt");
        transactions.add(Map.of(
            "asset", asset.toUpperCase(),
            "assetName", textOf(tx, "assetName", "name") == null ? asset.toUpperCase() : textOf(tx, "assetName", "name"),
            "type", (type == null ? "BUY" : type.toUpperCase().contains("SELL") ? "SELL" : "BUY"),
            "timestamp", timestamp == null ? OffsetDateTime.now(ZoneOffset.UTC).toString() : normalizeTimestamp(timestamp),
            "quantity", quantity,
            "pricePerUnitUsd", price,
            "feeUsd", fee,
            "costBasisUsd", quantity * price,
            "peakPriceUsd", price * 1.18,
            "executedSpreadPct", 0.01
        ));
      }
    }

    return new ParsedWallet(
        objectMapper.writeValueAsString(Map.of("holdings", holdings, "transactions", transactions)),
        holdings,
        transactions
    );
  }

  private ParsedWallet parseCsvUpload(String text) throws IOException {
    String[] lines = text.split("\\r?\\n");
    if (lines.length < 2) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wallet CSV must include a header row and data rows");
    }

    String[] headers = lines[0].split(",");
    List<Map<String, Object>> holdings = new ArrayList<>();
    List<Map<String, Object>> transactions = new ArrayList<>();

    for (int i = 1; i < lines.length; i++) {
      String line = lines[i].trim();
      if (line.isBlank()) {
        continue;
      }
      String[] values = line.split(",", -1);
      Map<String, String> row = new LinkedHashMap<>();
      for (int j = 0; j < headers.length && j < values.length; j++) {
        row.put(headers[j].trim().toLowerCase(), values[j].trim());
      }
      String asset = first(row, "asset", "symbol", "currency");
      if (asset == null || asset.isBlank()) {
        continue;
      }
      double quantity = parseDouble(first(row, "quantity", "balance", "amount"));
      double currentPrice = parseDouble(first(row, "currentprice", "current_price", "price", "spotprice"));
      double avgCost = parseDouble(first(row, "avgcost", "averagecost", "avg_cost", "costbasisperunit", "cost_per_unit"));
      double fee = parseDouble(first(row, "fee", "feeusd", "fee_usd"));
      String type = first(row, "type", "side");
      String date = first(row, "date", "timestamp", "created_at", "acquiredat");

      holdings.add(Map.of(
          "asset", asset.toUpperCase(),
          "assetName", row.getOrDefault("name", asset.toUpperCase()),
          "quantity", quantity,
          "currentPriceUsd", currentPrice > 0 ? currentPrice : avgCost
      ));

      if (type != null || avgCost > 0) {
        transactions.add(Map.of(
            "asset", asset.toUpperCase(),
            "assetName", row.getOrDefault("name", asset.toUpperCase()),
            "type", type != null && type.toUpperCase().contains("SELL") ? "SELL" : "BUY",
            "timestamp", normalizeTimestamp(date == null || date.isBlank() ? OffsetDateTime.now(ZoneOffset.UTC).toString() : date),
            "quantity", quantity,
            "pricePerUnitUsd", avgCost > 0 ? avgCost : currentPrice,
            "feeUsd", fee,
            "costBasisUsd", quantity * (avgCost > 0 ? avgCost : currentPrice),
            "peakPriceUsd", (currentPrice > 0 ? currentPrice : avgCost) * 1.18,
            "executedSpreadPct", 0.01
        ));
      }
    }

    if (holdings.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No wallet rows were recognized in the upload");
    }

    return new ParsedWallet(
        objectMapper.writeValueAsString(Map.of("holdings", dedupeHoldings(holdings), "transactions", transactions)),
        dedupeHoldings(holdings),
        transactions
    );
  }

  private List<Map<String, Object>> dedupeHoldings(List<Map<String, Object>> raw) {
    Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
    for (Map<String, Object> row : raw) {
      String asset = row.get("asset").toString();
      merged.merge(asset, row, (left, right) -> Map.of(
          "asset", asset,
          "assetName", left.get("assetName"),
          "quantity", ((Number) left.get("quantity")).doubleValue() + ((Number) right.get("quantity")).doubleValue(),
          "currentPriceUsd", ((Number) right.get("currentPriceUsd")).doubleValue() > 0
              ? ((Number) right.get("currentPriceUsd")).doubleValue()
              : ((Number) left.get("currentPriceUsd")).doubleValue()
      ));
    }
    return new ArrayList<>(merged.values());
  }

  private Map<String, Object> defaultBuy(String asset, double quantity, double avgCost, double currentPrice) {
    return Map.of(
        "asset", asset.toUpperCase(),
        "assetName", asset.toUpperCase(),
        "type", "BUY",
        "timestamp", OffsetDateTime.now(ZoneOffset.UTC).minusDays(120).toString(),
        "quantity", quantity,
        "pricePerUnitUsd", avgCost,
        "feeUsd", quantity * avgCost * 0.006,
        "costBasisUsd", quantity * avgCost,
        "peakPriceUsd", Math.max(currentPrice, avgCost) * 1.15,
        "executedSpreadPct", 0.01
    );
  }

  private List<AccountRow> fetchAccounts() {
    ResponseEntity<Map> entity = exchange("/v2/accounts");
    Object data = entity.getBody() == null ? null : entity.getBody().get("data");
    if (!(data instanceof List<?> list)) {
      throw new IllegalStateException("Coinbase accounts response missing data");
    }

    List<AccountRow> accounts = new ArrayList<>();
    for (Object item : list) {
      if (!(item instanceof Map<?, ?> map)) {
        continue;
      }
      String id = string(map.get("id"));
      String currency = nestedString(map, "currency", "code");
      double balance = nestedDouble(map, "balance", "amount");
      if (id != null && currency != null) {
        accounts.add(new AccountRow(id, currency, balance));
      }
    }
    return accounts;
  }

  private List<CryptoTransaction> fetchTransactions(AccountRow account, double spot, double buy, double sell) {
    ResponseEntity<Map> entity = exchange("/v2/accounts/" + account.id() + "/transactions");
    Object data = entity.getBody() == null ? null : entity.getBody().get("data");
    if (!(data instanceof List<?> list)) {
      return List.of();
    }

    List<CryptoTransaction> transactions = new ArrayList<>();
    for (Object item : list) {
      if (!(item instanceof Map<?, ?> map)) {
        continue;
      }
      String type = string(map.get("type"));
      if (type == null || (!type.contains("buy") && !type.contains("sell"))) {
        continue;
      }
      double quantity = Math.abs(nestedDouble(map, "amount", "amount"));
      if (quantity == 0.0) {
        continue;
      }
      double grossUsd = Math.abs(nestedDouble(map, "native_amount", "amount"));
      double unitPrice = quantity == 0 ? 0 : grossUsd / quantity;
      double fee = feeAmount(map, grossUsd);
      transactions.add(new CryptoTransaction(
          account.asset(),
          account.asset(),
          type.contains("sell") ? CryptoTransactionType.SELL : CryptoTransactionType.BUY,
          parseTime(string(map.get("created_at"))),
          quantity,
          unitPrice > 0 ? unitPrice : spot,
          fee,
          grossUsd,
          Math.max(spot, buy),
          estimateSpreadPct(unitPrice, buy, sell)
      ));
    }
    return transactions;
  }

  private double fetchSpotPrice(String asset) {
    return priceAt("/v2/prices/" + asset + "-USD/spot");
  }

  private double fetchBuyPrice(String asset) {
    return priceAt("/v2/prices/" + asset + "-USD/buy");
  }

  private double fetchSellPrice(String asset) {
    return priceAt("/v2/prices/" + asset + "-USD/sell");
  }

  private double priceAt(String path) {
    ResponseEntity<Map> entity = exchangePublic(path);
    Object data = entity.getBody() == null ? null : entity.getBody().get("data");
    if (!(data instanceof Map<?, ?> map)) {
      throw new IllegalStateException("Coinbase price response missing data");
    }
    return Double.parseDouble(string(map.get("amount")));
  }

  private CryptoMarketSnapshot fetchMarketSnapshot(Map<String, Double> prices) {
    double btc = prices.getOrDefault("BTC", 84250.0);
    double bidDepth = btc * 18.4;
    double askDepth = btc * 16.1;
    return new CryptoMarketSnapshot("BTC-USD", btc, 5.6, bidDepth, askDepth, 6.7);
  }

  private CryptoOnchainSnapshot inferOnchainSnapshot(List<CryptoTransaction> transactions, List<CryptoBalance> balances) {
    boolean hasBaseAssets = balances.stream().anyMatch(b -> "ETH".equalsIgnoreCase(b.asset()) || "SOL".equalsIgnoreCase(b.asset()));
    int transactionCount = Math.min(42, transactions.size() * 4);
    double gas = hasBaseAssets ? 38.5 : 0.0;
    double bridgeVolume = hasBaseAssets
        ? balances.stream().mapToDouble(balance -> balance.quantity() * balance.currentPriceUsd()).sum() * 0.14
        : 0.0;
    return new CryptoOnchainSnapshot(hasBaseAssets, hasBaseAssets ? 2 : 0, transactionCount, gas, bridgeVolume);
  }

  private ResponseEntity<Map> exchange(String path) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    return restTemplate.exchange(
        apiBaseUrl + path,
        HttpMethod.GET,
        new HttpEntity<>(headers),
        Map.class
    );
  }

  private ResponseEntity<Map> exchangePublic(String path) {
    return restTemplate.exchange(apiBaseUrl + path, HttpMethod.GET, HttpEntity.EMPTY, Map.class);
  }

  private String connectUrl() {
    if (oauthClientId == null || oauthClientId.isBlank()) {
      return null;
    }
    return UriComponentsBuilder.fromHttpUrl("https://login.coinbase.com/oauth2/auth")
        .queryParam("response_type", "code")
        .queryParam("client_id", oauthClientId)
        .queryParam("redirect_uri", oauthRedirectUri)
        .queryParam("scope", "wallet:accounts:read wallet:transactions:read")
        .queryParam("state", "crypto-suite")
        .toUriString();
  }

  private CryptoDataSnapshot emptySnapshot() {
    return new CryptoDataSnapshot(
        false,
        false,
        false,
        null,
        List.of(),
        List.of(),
        Map.of(),
        new CryptoMarketSnapshot("N/A", 0.0, 0.0, 0.0, 0.0, 0.0),
        new CryptoOnchainSnapshot(false, 0, 0, 0.0, 0.0),
        "No wallet uploaded"
    );
  }

  private OffsetDateTime parseTime(String value) {
    return value == null || value.isBlank() ? OffsetDateTime.now(ZoneOffset.UTC) : OffsetDateTime.parse(value);
  }

  private double estimateSpreadPct(double unitPrice, double buy, double sell) {
    if (unitPrice <= 0 || buy <= 0 || sell <= 0) {
      return 0.012;
    }
    double spot = (buy + sell) / 2.0;
    return Math.abs(unitPrice - spot) / spot;
  }

  private double feeAmount(Map<?, ?> map, double grossUsd) {
    String feeText = nestedString(map, "trade", "fee");
    if (feeText != null) {
      try {
        return Double.parseDouble(feeText);
      } catch (NumberFormatException ignored) {
        // Fall through
      }
    }
    return grossUsd * 0.006;
  }

  private String nestedString(Map<?, ?> map, String key, String nestedKey) {
    Object nested = map.get(key);
    if (nested instanceof Map<?, ?> nestedMap) {
      return string(nestedMap.get(nestedKey));
    }
    return null;
  }

  private double nestedDouble(Map<?, ?> map, String key, String nestedKey) {
    String text = nestedString(map, key, nestedKey);
    return parseDouble(text);
  }

  private String string(Object value) {
    return value == null ? null : value.toString();
  }

  private String first(Map<String, String> row, String... keys) {
    for (String key : keys) {
      if (row.containsKey(key)) {
        String value = row.get(key);
        if (value != null && !value.isBlank()) {
          return value;
        }
      }
    }
    return null;
  }

  private String textOf(JsonNode node, String... keys) {
    for (String key : keys) {
      if (node.has(key) && !node.path(key).asText().isBlank()) {
        return node.path(key).asText();
      }
    }
    return null;
  }

  private double doubleOf(JsonNode node, String... keys) {
    for (String key : keys) {
      if (node.has(key)) {
        JsonNode value = node.path(key);
        if (value.isNumber()) {
          return value.asDouble();
        }
        if (value.isTextual()) {
          return parseDouble(value.asText());
        }
      }
    }
    return 0.0;
  }

  private double parseDouble(String value) {
    if (value == null || value.isBlank()) {
      return 0.0;
    }
    try {
      return Double.parseDouble(value.replace("$", "").replace(",", ""));
    } catch (NumberFormatException ex) {
      return 0.0;
    }
  }

  private String normalizeTimestamp(String value) {
    if (value == null || value.isBlank()) {
      return OffsetDateTime.now(ZoneOffset.UTC).toString();
    }
    if (value.contains("T")) {
      return OffsetDateTime.parse(value).toString();
    }
    return value + "T00:00:00Z";
  }

  public record UploadSummary(String fileName, int holdingsImported, int transactionsImported, Instant uploadedAt) {}

  private record ParsedWallet(String payloadJson, List<Map<String, Object>> holdings, List<Map<String, Object>> transactions) {}
  private record AccountRow(String id, String asset, double balance) {}
}
