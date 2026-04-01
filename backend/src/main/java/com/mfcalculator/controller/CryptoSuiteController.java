package com.mfcalculator.controller;

import com.mfcalculator.dto.CryptoUploadResponse;
import com.mfcalculator.dto.CryptoSuiteResponse;
import com.mfcalculator.security.UserPrincipal;
import com.mfcalculator.service.CryptoSuiteService;
import com.mfcalculator.service.CoinbaseCryptoDataClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/crypto")
public class CryptoSuiteController {
  private final CryptoSuiteService cryptoSuiteService;
  private final CoinbaseCryptoDataClient coinbaseCryptoDataClient;

  public CryptoSuiteController(
      CryptoSuiteService cryptoSuiteService,
      CoinbaseCryptoDataClient coinbaseCryptoDataClient
  ) {
    this.cryptoSuiteService = cryptoSuiteService;
    this.coinbaseCryptoDataClient = coinbaseCryptoDataClient;
  }

  private static Long getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authenticated");
    }
    return ((UserPrincipal) auth.getPrincipal()).getUserId();
  }

  @GetMapping("/suite")
  public CryptoSuiteResponse suite() {
    return cryptoSuiteService.buildSuite(getCurrentUserId());
  }

  @PostMapping("/upload")
  public ResponseEntity<CryptoUploadResponse> uploadWallet(@RequestParam("file") MultipartFile file) {
    Long userId = getCurrentUserId();
    CoinbaseCryptoDataClient.UploadSummary summary = coinbaseCryptoDataClient.saveWalletUpload(userId, file);
    CryptoSuiteResponse suite = cryptoSuiteService.buildSuite(userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(new CryptoUploadResponse(
        summary.fileName(),
        summary.holdingsImported(),
        summary.transactionsImported(),
        summary.uploadedAt().toString(),
        suite
    ));
  }
}
