package com.mfcalculator.security;

import java.util.Collection;
import java.util.Collections;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Principal stored in SecurityContext after JWT validation. Holds the current user's id and username.
 */
public class UserPrincipal implements Authentication {

  private final Long userId;
  private final String username;
  private boolean authenticated = true;

  public UserPrincipal(Long userId, String username) {
    this.userId = userId;
    this.username = username;
  }

  public Long getUserId() {
    return userId;
  }

  @Override
  public String getName() {
    return username;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.emptyList();
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public Object getDetails() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return this;
  }

  @Override
  public boolean isAuthenticated() {
    return authenticated;
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) {
    this.authenticated = isAuthenticated;
  }
}
