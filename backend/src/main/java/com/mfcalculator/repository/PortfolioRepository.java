package com.mfcalculator.repository;

import com.mfcalculator.model.Portfolio;
import com.mfcalculator.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

  List<Portfolio> findByUserOrderByCreatedAtDesc(User user);
}
