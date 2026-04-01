package com.mfcalculator.controller;

import com.mfcalculator.dto.ChangePasswordRequest;
import com.mfcalculator.dto.UpdateProfileRequest;
import com.mfcalculator.dto.UserProfileResponse;
import com.mfcalculator.model.User;
import com.mfcalculator.repository.UserRepository;
import com.mfcalculator.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/user/profile")
public class UserProfileController {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserProfileController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  private User getCurrentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authenticated");
    }
    Long userId = ((UserPrincipal) auth.getPrincipal()).getUserId();
    return userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
  }

  @GetMapping
  public UserProfileResponse getProfile() {
    User user = getCurrentUser();
    return new UserProfileResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getCreatedAt());
  }

  @PutMapping
  public UserProfileResponse updateProfile(@RequestBody UpdateProfileRequest request) {
    User user = getCurrentUser();
    if (request.displayName() != null && !request.displayName().isBlank()) {
      user.setDisplayName(request.displayName().trim());
    }
    userRepository.save(user);
    return new UserProfileResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getCreatedAt());
  }

  @PutMapping("/password")
  public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request) {
    User user = getCurrentUser();
    if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
    }
    if (request.newPassword() == null || request.newPassword().length() < 8) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be at least 8 characters");
    }
    user.setPassword(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  public ResponseEntity<Void> deleteAccount() {
    User user = getCurrentUser();
    userRepository.delete(user);
    return ResponseEntity.noContent().build();
  }
}
