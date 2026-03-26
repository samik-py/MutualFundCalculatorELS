package com.mfcalculator.controller;

import com.mfcalculator.dto.DashboardResponse;
import com.mfcalculator.security.UserPrincipal;
import com.mfcalculator.service.UserPortfolioService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/user")
public class UserDashboardController {

  private final UserPortfolioService userPortfolioService;

  public UserDashboardController(UserPortfolioService userPortfolioService) {
    this.userPortfolioService = userPortfolioService;
  }

  private static Long getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authenticated");
    }
    return ((UserPrincipal) auth.getPrincipal()).getUserId();
  }

  @GetMapping("/dashboard")
  public DashboardResponse getDashboard() {
    Long userId = getCurrentUserId();
    return userPortfolioService.getDashboardPerformance(userId);
  }
}
