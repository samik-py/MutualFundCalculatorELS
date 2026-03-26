package com.mfcalculator.controller;

import com.mfcalculator.dto.AddHoldingRequest;
import com.mfcalculator.dto.CreatePortfolioRequest;
import com.mfcalculator.dto.PortfolioDetailResponse;
import com.mfcalculator.dto.PortfolioPerformanceResponse;
import com.mfcalculator.dto.PortfolioSummaryResponse;
import com.mfcalculator.security.UserPrincipal;
import com.mfcalculator.service.UserPortfolioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/user/portfolios")
public class UserPortfolioController {

  private final UserPortfolioService userPortfolioService;

  public UserPortfolioController(UserPortfolioService userPortfolioService) {
    this.userPortfolioService = userPortfolioService;
  }

  private static Long getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authenticated");
    }
    return ((UserPrincipal) auth.getPrincipal()).getUserId();
  }

  @GetMapping
  public List<PortfolioSummaryResponse> listPortfolios() {
    Long userId = getCurrentUserId();
    return userPortfolioService.listPortfolios(userId);
  }

  @PostMapping
  public ResponseEntity<PortfolioDetailResponse> createPortfolio(@Valid @RequestBody CreatePortfolioRequest request) {
    Long userId = getCurrentUserId();
    PortfolioDetailResponse created = userPortfolioService.createPortfolio(userId, request.name());
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @GetMapping("/{id}")
  public PortfolioDetailResponse getPortfolio(@PathVariable Long id) {
    Long userId = getCurrentUserId();
    return userPortfolioService.getPortfolio(userId, id);
  }

  @GetMapping("/{id}/performance")
  public PortfolioPerformanceResponse getPortfolioPerformance(@PathVariable Long id) {
    Long userId = getCurrentUserId();
    return userPortfolioService.getPortfolioPerformance(userId, id);
  }

  @PutMapping("/{id}")
  public PortfolioDetailResponse updatePortfolio(@PathVariable Long id, @Valid @RequestBody CreatePortfolioRequest request) {
    Long userId = getCurrentUserId();
    return userPortfolioService.updatePortfolio(userId, id, request.name());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deletePortfolio(@PathVariable Long id) {
    Long userId = getCurrentUserId();
    userPortfolioService.deletePortfolio(userId, id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/holdings")
  public ResponseEntity<PortfolioDetailResponse> addHolding(
      @PathVariable Long id,
      @Valid @RequestBody AddHoldingRequest request) {
    Long userId = getCurrentUserId();
    PortfolioDetailResponse updated = userPortfolioService.addHolding(userId, id, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(updated);
  }

  @DeleteMapping("/{id}/holdings/{holdingId}")
  public ResponseEntity<Void> removeHolding(@PathVariable Long id, @PathVariable Long holdingId) {
    Long userId = getCurrentUserId();
    userPortfolioService.removeHolding(userId, id, holdingId);
    return ResponseEntity.noContent().build();
  }
}
