package com.mfcalculator.service;

import com.mfcalculator.dto.AddHoldingRequest;
import com.mfcalculator.dto.HoldingDetailResponse;
import com.mfcalculator.dto.HoldingPerformanceItem;
import com.mfcalculator.dto.PortfolioDetailResponse;
import com.mfcalculator.dto.DashboardResponse;
import com.mfcalculator.dto.PortfolioPerformanceResponse;
import com.mfcalculator.dto.PortfolioSummaryResponse;
import com.mfcalculator.model.Portfolio;
import com.mfcalculator.model.PortfolioHolding;
import com.mfcalculator.model.User;
import com.mfcalculator.repository.PortfolioHoldingRepository;
import com.mfcalculator.repository.PortfolioRepository;
import com.mfcalculator.repository.UserRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserPortfolioService {

  private static final Map<String, FundDisplay> FUND_DISPLAY = Map.of(
      "vanguard-500", new FundDisplay("Vanguard 500 Index", "VFIAX"),
      "fidelity-growth", new FundDisplay("Fidelity Growth Company", "FDGRX"),
      "trowe-bluechip", new FundDisplay("T. Rowe Price Blue Chip", "TRBCX"),
      "schwab-total", new FundDisplay("Schwab Total Market", "SWTSX"),
      "pimco-total", new FundDisplay("PIMCO Total Return", "PTTRX")
  );

  private final UserRepository userRepository;
  private final PortfolioRepository portfolioRepository;
  private final PortfolioHoldingRepository portfolioHoldingRepository;
  private final FinanceService financeService;

  public UserPortfolioService(
      UserRepository userRepository,
      PortfolioRepository portfolioRepository,
      PortfolioHoldingRepository portfolioHoldingRepository,
      FinanceService financeService) {
    this.userRepository = userRepository;
    this.portfolioRepository = portfolioRepository;
    this.portfolioHoldingRepository = portfolioHoldingRepository;
    this.financeService = financeService;
  }

  private static final class FundDisplay {
    final String name;
    final String ticker;

    FundDisplay(String name, String ticker) {
      this.name = name;
      this.ticker = ticker;
    }
  }

  @Transactional(readOnly = true)
  public List<PortfolioSummaryResponse> listPortfolios(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
    return portfolioRepository.findByUserOrderByCreatedAtDesc(user).stream()
        .map(p -> new PortfolioSummaryResponse(
            p.getId(),
            p.getName(),
            p.getHoldings().size(),
            p.getCreatedAt()))
        .collect(Collectors.toList());
  }

  @Transactional
  public PortfolioDetailResponse createPortfolio(Long userId, String name) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
    Portfolio portfolio = new Portfolio(user, name);
    portfolio = portfolioRepository.save(portfolio);
    return toDetail(portfolio);
  }

  @Transactional(readOnly = true)
  public PortfolioDetailResponse getPortfolio(Long userId, Long portfolioId) {
    Portfolio portfolio = portfolioRepository.findById(portfolioId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));
    if (!portfolio.getUser().getId().equals(userId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }
    return toDetail(portfolio);
  }

  @Transactional
  public PortfolioDetailResponse updatePortfolio(Long userId, Long portfolioId, String name) {
    Portfolio portfolio = portfolioRepository.findById(portfolioId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));
    if (!portfolio.getUser().getId().equals(userId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }
    portfolio.setName(name);
    portfolio = portfolioRepository.save(portfolio);
    return toDetail(portfolio);
  }

  @Transactional
  public void deletePortfolio(Long userId, Long portfolioId) {
    Portfolio portfolio = portfolioRepository.findById(portfolioId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));
    if (!portfolio.getUser().getId().equals(userId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }
    portfolioRepository.delete(portfolio);
  }

  @Transactional
  public PortfolioDetailResponse addHolding(Long userId, Long portfolioId, AddHoldingRequest request) {
    Portfolio portfolio = portfolioRepository.findById(portfolioId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));
    if (!portfolio.getUser().getId().equals(userId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }
    PortfolioHolding holding = new PortfolioHolding(
        portfolio,
        request.fundId(),
        request.shares(),
        request.purchasePrice(),
        request.purchaseDate());
    holding = portfolioHoldingRepository.save(holding);
    portfolio.getHoldings().add(holding);
    return toDetail(portfolio);
  }

  @Transactional
  public void removeHolding(Long userId, Long portfolioId, Long holdingId) {
    PortfolioHolding holding = portfolioHoldingRepository.findById(holdingId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Holding not found"));
    Portfolio portfolio = holding.getPortfolio();
    if (!portfolio.getUser().getId().equals(userId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }
    if (!portfolio.getId().equals(portfolioId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Holding does not belong to this portfolio");
    }
    portfolio.getHoldings().remove(holding);
    portfolioHoldingRepository.delete(holding);
  }

  @Transactional(readOnly = true)
  public PortfolioPerformanceResponse getPortfolioPerformance(Long userId, Long portfolioId) {
    Portfolio portfolio = portfolioRepository.findById(portfolioId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));
    if (!portfolio.getUser().getId().equals(userId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }
    return computePerformance(portfolio.getHoldings());
  }

  @Transactional(readOnly = true)
  public DashboardResponse getDashboardPerformance(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
    List<Portfolio> portfolios = portfolioRepository.findByUserOrderByCreatedAtDesc(user);
    List<PortfolioHolding> allHoldings = portfolios.stream()
        .flatMap(p -> p.getHoldings().stream())
        .collect(Collectors.toList());
    PortfolioPerformanceResponse perf = computePerformance(allHoldings);
    return new DashboardResponse(
        perf.totalCostBasis(),
        perf.totalCurrentValue(),
        perf.totalGainLoss(),
        perf.totalReturnPct()
    );
  }

  private PortfolioPerformanceResponse computePerformance(List<PortfolioHolding> holdings) {
    LocalDate today = LocalDate.now();
    List<HoldingPerformanceItem> items = holdings.stream()
        .map(h -> {
          long daysHeld = ChronoUnit.DAYS.between(h.getPurchaseDate(), today);
          double yearsHeld = daysHeld / 365.25;
          double annualReturn = financeService.annualReturnFor(h.getFundId());
          double costBasis = h.getPurchasePrice() * h.getShares();
          double currentValue = costBasis * Math.exp(annualReturn * yearsHeld);
          double gainLoss = currentValue - costBasis;
          double returnPct = costBasis != 0 ? (gainLoss / costBasis) * 100 : 0;
          FundDisplay display = FUND_DISPLAY.getOrDefault(h.getFundId(), new FundDisplay(h.getFundId(), h.getFundId()));
          return new HoldingPerformanceItem(
              h.getFundId(),
              display.name,
              display.ticker,
              h.getShares(),
              costBasis,
              currentValue,
              gainLoss,
              returnPct
          );
        })
        .collect(Collectors.toList());
    double totalCostBasis = items.stream().mapToDouble(HoldingPerformanceItem::costBasis).sum();
    double totalCurrentValue = items.stream().mapToDouble(HoldingPerformanceItem::currentValue).sum();
    double totalGainLoss = totalCurrentValue - totalCostBasis;
    double totalReturnPct = totalCostBasis != 0 ? (totalGainLoss / totalCostBasis) * 100 : 0;
    return new PortfolioPerformanceResponse(items, totalCostBasis, totalCurrentValue, totalGainLoss, totalReturnPct);
  }

  private static PortfolioDetailResponse toDetail(Portfolio portfolio) {
    List<HoldingDetailResponse> holdings = portfolio.getHoldings().stream()
        .map(h -> new HoldingDetailResponse(
            h.getId(),
            h.getFundId(),
            h.getShares(),
            h.getPurchasePrice(),
            h.getPurchaseDate()))
        .collect(Collectors.toList());
    return new PortfolioDetailResponse(
        portfolio.getId(),
        portfolio.getName(),
        holdings,
        portfolio.getCreatedAt());
  }
}
