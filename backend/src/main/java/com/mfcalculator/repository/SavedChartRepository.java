package com.mfcalculator.repository;

import com.mfcalculator.model.SavedChart;
import com.mfcalculator.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedChartRepository extends JpaRepository<SavedChart, Long> {

  List<SavedChart> findByUserOrderByCreatedAtDesc(User user);
}
