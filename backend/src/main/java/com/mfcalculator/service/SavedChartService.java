package com.mfcalculator.service;

import com.mfcalculator.dto.SavedChartResponse;
import com.mfcalculator.model.SavedChart;
import com.mfcalculator.model.User;
import com.mfcalculator.repository.SavedChartRepository;
import com.mfcalculator.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SavedChartService {

  private final UserRepository userRepository;
  private final SavedChartRepository savedChartRepository;

  public SavedChartService(UserRepository userRepository, SavedChartRepository savedChartRepository) {
    this.userRepository = userRepository;
    this.savedChartRepository = savedChartRepository;
  }

  public List<SavedChartResponse> listCharts(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
    return savedChartRepository.findByUserOrderByCreatedAtDesc(user).stream()
        .map(SavedChartService::toResponse)
        .toList();
  }

  @Transactional
  public SavedChartResponse saveChart(Long userId, String title, String fundIds, int timeHorizon, double amount) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
    SavedChart chart = new SavedChart(user, title, fundIds, timeHorizon, amount);
    chart = savedChartRepository.save(chart);
    return toResponse(chart);
  }

  public SavedChartResponse getChart(Long userId, Long chartId) {
    SavedChart chart = savedChartRepository.findById(chartId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chart not found"));
    if (!chart.getUser().getId().equals(userId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }
    return toResponse(chart);
  }

  @Transactional
  public void deleteChart(Long userId, Long chartId) {
    SavedChart chart = savedChartRepository.findById(chartId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chart not found"));
    if (!chart.getUser().getId().equals(userId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }
    savedChartRepository.delete(chart);
  }

  private static SavedChartResponse toResponse(SavedChart chart) {
    return new SavedChartResponse(
        chart.getId(),
        chart.getTitle(),
        chart.getFundIds(),
        chart.getTimeHorizon(),
        chart.getAmount(),
        chart.getCreatedAt()
    );
  }
}
