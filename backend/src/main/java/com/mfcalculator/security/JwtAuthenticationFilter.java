package com.mfcalculator.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.mfcalculator.repository.UserRepository;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;

  public JwtAuthenticationFilter(
      JwtUtil jwtUtil,
      UserRepository userRepository) {
    this.jwtUtil = jwtUtil;
    this.userRepository = userRepository;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(7);
    if (jwtUtil.isTokenValid(token)
        && SecurityContextHolder.getContext().getAuthentication() == null) {
      String username = jwtUtil.extractUsername(token);
      Optional<com.mfcalculator.model.User> userOpt = userRepository.findByUsername(username);
      if (userOpt.isPresent()) {
        com.mfcalculator.model.User user = userOpt.get();
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getUsername());
        principal.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(principal);
      } else {
        // Keep unauthenticated if user doesn't exist anymore
      }
    }

    filterChain.doFilter(request, response);
  }
}
