package com.mfcalculator.controller;

import com.mfcalculator.dto.SavedChartResponse;
import com.mfcalculator.dto.SaveChartRequest;
import com.mfcalculator.security.UserPrincipal;
import com.mfcalculator.service.SavedChartService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/user/charts")
public class SavedChartController {

  private final SavedChartService savedChartService;

  public SavedChartController(SavedChartService savedChartService) {
    this.savedChartService = savedChartService;
  }

  private static Long getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authenticated");
    }
    return ((UserPrincipal) auth.getPrincipal()).getUserId();
  }

  @GetMapping
  public List<SavedChartResponse> listCharts() {
    Long userId = getCurrentUserId();
    return savedChartService.listCharts(userId);
  }

  @PostMapping
  public ResponseEntity<SavedChartResponse> saveChart(@Valid @RequestBody SaveChartRequest request) {
    Long userId = getCurrentUserId();
    SavedChartResponse saved = savedChartService.saveChart(
        userId,
        request.title(),
        request.fundIds(),
        request.timeHorizon(),
        request.amount()
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
  }

  @GetMapping("/{id}")
  public SavedChartResponse getChart(@PathVariable Long id) {
    Long userId = getCurrentUserId();
    return savedChartService.getChart(userId, id);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteChart(@PathVariable Long id) {
    Long userId = getCurrentUserId();
    savedChartService.deleteChart(userId, id);
    return ResponseEntity.noContent().build();
  }
}
