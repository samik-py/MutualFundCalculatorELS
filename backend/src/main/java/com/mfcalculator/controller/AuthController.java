package com.mfcalculator.controller;

import com.mfcalculator.dto.AuthResponse;
import com.mfcalculator.dto.LoginRequest;
import com.mfcalculator.dto.RegisterRequest;
import com.mfcalculator.model.User;
import com.mfcalculator.repository.UserRepository;
import com.mfcalculator.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;

  public AuthController(UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtUtil jwtUtil) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
  }

  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    if (userRepository.findByUsername(request.getEmail()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
    }

    User user = new User(request.getEmail(), passwordEncoder.encode(request.getPassword()));
    user.setDisplayName(request.getDisplayName());
    userRepository.save(user);

    String token = jwtUtil.generateToken(user.getUsername());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new AuthResponse(token, user.getUsername(), user.getDisplayName()));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    User user = userRepository.findByUsername(request.getEmail())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    String token = jwtUtil.generateToken(user.getUsername());
    return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getDisplayName()));
  }
}
