package com.mfcalculator.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "saved_charts")
public class SavedChart {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private String title;

  @Column(name = "fund_ids", nullable = false, length = 512)
  private String fundIds;

  @Column(name = "time_horizon", nullable = false)
  private int timeHorizon;

  @Column(nullable = false)
  private double amount;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public SavedChart() {}

  public SavedChart(User user, String title, String fundIds, int timeHorizon, double amount) {
    this.user = user;
    this.title = title;
    this.fundIds = fundIds;
    this.timeHorizon = timeHorizon;
    this.amount = amount;
    this.createdAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getFundIds() {
    return fundIds;
  }

  public void setFundIds(String fundIds) {
    this.fundIds = fundIds;
  }

  public int getTimeHorizon() {
    return timeHorizon;
  }

  public void setTimeHorizon(int timeHorizon) {
    this.timeHorizon = timeHorizon;
  }

  public double getAmount() {
    return amount;
  }

  public void setAmount(double amount) {
    this.amount = amount;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
