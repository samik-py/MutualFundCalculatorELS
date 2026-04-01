package com.mfcalculator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mfcalculator.model.CryptoWalletUpload;
import com.mfcalculator.repository.CryptoWalletUploadRepository;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.mock.web.MockMultipartFile;

class CoinbaseCryptoDataClientTest {
  private static final String WALLET_JSON = """
      {
        "holdings": [
          {
            "asset": "ETH",
            "name": "Ethereum",
            "quantity": 2.5,
            "currentPriceUSD": 3520.15,
            "avgCostBasisUSD": 2895.10,
            "lots": [
              {
                "purchaseDate": "2024-03-03",
                "quantity": 1.0,
                "unitCostUSD": 2400.00
              },
              {
                "purchaseDate": "2024-03-15",
                "quantity": 1.5,
                "unitCostUSD": 3225.17
              }
            ]
          },
          {
            "asset": "BTC",
            "name": "Bitcoin",
            "quantity": 0.4,
            "currentPriceUSD": 68250.00,
            "avgCostBasisUSD": 60110.00
          }
        ]
      }
      """;

  @Test
  void saveWalletUploadAcceptsWalletJsonShape() throws IOException {
    AtomicReference<CryptoWalletUpload> storedUpload = new AtomicReference<>();
    CryptoWalletUploadRepository repository = (CryptoWalletUploadRepository) Proxy.newProxyInstance(
        CryptoWalletUploadRepository.class.getClassLoader(),
        new Class<?>[]{CryptoWalletUploadRepository.class},
        (proxy, method, args) -> {
          if ("save".equals(method.getName())) {
            CryptoWalletUpload upload = (CryptoWalletUpload) args[0];
            storedUpload.set(upload);
            return upload;
          }
          if ("findTopByUserIdOrderByUploadedAtDesc".equals(method.getName())) {
            return Optional.ofNullable(storedUpload.get());
          }
          if ("findById".equals(method.getName())) {
            return Optional.empty();
          }
          if ("count".equals(method.getName())) {
            return 0L;
          }
          if ("existsById".equals(method.getName()) || "exists".equals(method.getName())) {
            return false;
          }
          if ("findAll".equals(method.getName())) {
            return java.util.List.of();
          }
          return null;
        }
    );

    CoinbaseCryptoDataClient client = new CoinbaseCryptoDataClient(
        new RestTemplateBuilder(),
        new ObjectMapper(),
        repository,
        "https://api.coinbase.com",
        "",
        "",
        "http://localhost:5173/crypto",
        20
    );

    MockMultipartFile file = new MockMultipartFile(
        "file",
        "wallet.json",
        "application/json",
        WALLET_JSON.getBytes(StandardCharsets.UTF_8)
    );

    CoinbaseCryptoDataClient.UploadSummary summary = client.saveWalletUpload(42L, file);

    assertNotNull(summary);
    assertTrue(summary.holdingsImported() > 0);
    assertTrue(summary.transactionsImported() > 0);
    assertEquals("wallet.json", summary.fileName());

    CryptoSuiteService.CryptoDataSnapshot snapshot = client.fetchSnapshot(42L);
    assertTrue(snapshot.uploadedWallet());
    assertEquals("Uploaded wallet file", snapshot.sourceLabel());
    assertEquals(2, snapshot.balances().size());
    assertEquals("ETH", snapshot.balances().get(0).asset());
  }
}
