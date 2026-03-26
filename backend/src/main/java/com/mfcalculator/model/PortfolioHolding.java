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
import java.time.LocalDate;

@Entity
@Table(name = "portfolio_holdings")
public class PortfolioHolding {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "portfolio_id", nullable = false)
  private Portfolio portfolio;

  @Column(name = "fund_id", nullable = false)
  private String fundId;

  @Column(nullable = false)
  private double shares;

  @Column(name = "purchase_price", nullable = false)
  private double purchasePrice;

  @Column(name = "purchase_date", nullable = false)
  private LocalDate purchaseDate;

  public PortfolioHolding() {}

  public PortfolioHolding(Portfolio portfolio, String fundId, double shares, double purchasePrice, LocalDate purchaseDate) {
    this.portfolio = portfolio;
    this.fundId = fundId;
    this.shares = shares;
    this.purchasePrice = purchasePrice;
    this.purchaseDate = purchaseDate;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Portfolio getPortfolio() {
    return portfolio;
  }

  public void setPortfolio(Portfolio portfolio) {
    this.portfolio = portfolio;
  }

  public String getFundId() {
    return fundId;
  }

  public void setFundId(String fundId) {
    this.fundId = fundId;
  }

  public double getShares() {
    return shares;
  }

  public void setShares(double shares) {
    this.shares = shares;
  }

  public double getPurchasePrice() {
    return purchasePrice;
  }

  public void setPurchasePrice(double purchasePrice) {
    this.purchasePrice = purchasePrice;
  }

  public LocalDate getPurchaseDate() {
    return purchaseDate;
  }

  public void setPurchaseDate(LocalDate purchaseDate) {
    this.purchaseDate = purchaseDate;
  }
}
