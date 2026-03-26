package com.mfcalculator.repository;

import com.mfcalculator.model.PortfolioHolding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioHoldingRepository extends JpaRepository<PortfolioHolding, Long> {}