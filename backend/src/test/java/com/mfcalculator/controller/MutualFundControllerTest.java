package com.mfcalculator.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mfcalculator.dto.*;
import com.mfcalculator.service.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MutualFundControllerTest {

  private static final double RF   = 0.04;
  private static final double RM   = 0.10;
  private static final double BETA = 1.0;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    FundCatalogService catalog = new FundCatalogService();
    FinanceService finance = new FinanceService(
        ticker -> BETA, catalog, () -> RF, ticker -> RM);
    CompareService compare = new CompareService(finance, catalog);
    MonteCarloService monteCarlo = new MonteCarloService(finance, ticker -> BETA, catalog);

    MutualFundController controller = new MutualFundController(
        finance,
        new PortfolioService(),
        catalog,
        compare,
        monteCarlo,
        () -> RF,
        () -> RM
    );

    mockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setMessageConverters(new MappingJackson2HttpMessageConverter())
        .build();
  }

  @Test
  void getFundsReturns200() throws Exception {
    mockMvc.perform(get("/api/funds"))
        .andExpect(status().isOk());
  }

  @Test
  void getFundsReturnsNonEmptyArray() throws Exception {
    String body = mockMvc.perform(get("/api/funds"))
        .andReturn().getResponse().getContentAsString();
    FundOption[] funds = objectMapper.readValue(body, FundOption[].class);
    assertTrue(funds.length > 0);
  }

  @Test
  void getFundsReturns21Funds() throws Exception {
    String body = mockMvc.perform(get("/api/funds"))
        .andReturn().getResponse().getContentAsString();
    FundOption[] funds = objectMapper.readValue(body, FundOption[].class);
    assertEquals(21, funds.length);
  }

  @Test
  void calculateReturns200ForValidRequest() throws Exception {
    String payload = objectMapper.writeValueAsString(
        new InvestmentRequest("vanguard-500", 10_000.0, 10));
    mockMvc.perform(post("/api/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isOk());
  }

  @Test
  void calculateReturnsCorrectFutureValue() throws Exception {
    double amount   = 10_000.0;
    int    years    = 10;
    double rate     = RF + BETA * (RM - RF);
    double expected = amount * Math.exp(rate * years);

    String payload = objectMapper.writeValueAsString(
        new InvestmentRequest("vanguard-500", amount, years));
    MvcResult result = mockMvc.perform(post("/api/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andReturn();

    InvestmentResponse response = objectMapper.readValue(
        result.getResponse().getContentAsString(), InvestmentResponse.class);
    assertEquals(expected, response.futureValue(), 1e-6);
  }

  @Test
  void calculateReturns400ForMissingFundId() throws Exception {
    String payload = "{\"amount\": 1000, \"years\": 5}";
    mockMvc.perform(post("/api/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isBadRequest());
  }

  @Test
  void calculateReturns400ForNegativeAmount() throws Exception {
    String payload = objectMapper.writeValueAsString(
        new InvestmentRequest("vanguard-500", -500.0, 5));
    mockMvc.perform(post("/api/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isBadRequest());
  }

  @Test
  void calculateReturns400ForNullYears() throws Exception {
    String payload = "{\"fundId\": \"vanguard-500\", \"amount\": 1000}";
    mockMvc.perform(post("/api/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isBadRequest());
  }

  @Test
  void calculateReturns400ForMalformedJson() throws Exception {
    mockMvc.perform(post("/api/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{ this is not json }"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void compareReturns200ForValidRequest() throws Exception {
    String payload = objectMapper.writeValueAsString(
        new CompareRequest(List.of("vanguard-500", "qqq"), 10_000.0, 5));
    mockMvc.perform(post("/api/compare")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isOk());
  }

  @Test
  void compareResponseContainsAllRequestedFunds() throws Exception {
    List<String> fundIds = List.of("vanguard-500", "qqq", "pimco-total");
    String payload = objectMapper.writeValueAsString(
        new CompareRequest(fundIds, 10_000.0, 5));
    MvcResult result = mockMvc.perform(post("/api/compare")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andReturn();

    CompareResponse response = objectMapper.readValue(
        result.getResponse().getContentAsString(), CompareResponse.class);
    assertEquals(3, response.funds().size());
  }

  @Test
  void compareReturns400ForEmptyFundIds() throws Exception {
    String payload = objectMapper.writeValueAsString(
        new CompareRequest(List.of(), 10_000.0, 5));
    mockMvc.perform(post("/api/compare")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isBadRequest());
  }

  @Test
  void marketIndicatorsReturns200() throws Exception {
    mockMvc.perform(get("/api/market/indicators"))
        .andExpect(status().isOk());
  }

  @Test
  void marketIndicatorsReturnsCorrectRf() throws Exception {
    String body = mockMvc.perform(get("/api/market/indicators"))
        .andReturn().getResponse().getContentAsString();
    MarketIndicatorsResponse resp =
        objectMapper.readValue(body, MarketIndicatorsResponse.class);
    assertEquals(RF, resp.riskFreeRate(), 1e-9);
  }

  @Test
  void marketIndicatorsReturnsCorrectRm() throws Exception {
    String body = mockMvc.perform(get("/api/market/indicators"))
        .andReturn().getResponse().getContentAsString();
    MarketIndicatorsResponse resp =
        objectMapper.readValue(body, MarketIndicatorsResponse.class);
    assertEquals(RM, resp.marketReturn5y(), 1e-9);
  }

  @Test
  void marketIndicatorsReturnsNonNullDataAsOf() throws Exception {
    String body = mockMvc.perform(get("/api/market/indicators"))
        .andReturn().getResponse().getContentAsString();
    MarketIndicatorsResponse resp =
        objectMapper.readValue(body, MarketIndicatorsResponse.class);
    assertNotNull(resp.dataAsOf());
    assertFalse(resp.dataAsOf().isBlank());
  }

  @Test
  void monteCarloReturns200ForValidRequest() throws Exception {
    String payload = objectMapper.writeValueAsString(
        new MonteCarloRequest("vanguard-500", 10_000.0, 5, 200));
    mockMvc.perform(post("/api/monte-carlo")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isOk());
  }

  @Test
  void monteCarloResponseHasFivePercentileSeries() throws Exception {
    String payload = objectMapper.writeValueAsString(
        new MonteCarloRequest("vanguard-500", 10_000.0, 5, 200));
    MvcResult result = mockMvc.perform(post("/api/monte-carlo")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andReturn();
    MonteCarloResponse response = objectMapper.readValue(
        result.getResponse().getContentAsString(), MonteCarloResponse.class);
    assertEquals(5, response.series().size());
  }

  @Test
  void monteCarloReturns400ForMissingFundId() throws Exception {
    String payload = "{\"amount\": 1000, \"years\": 5}";
    mockMvc.perform(post("/api/monte-carlo")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isBadRequest());
  }

  @Test
  void portfolioCompareReturns200ForValidRequest() throws Exception {
    List<PortfolioHolding> portA = List.of(new PortfolioHolding("vanguard-500", 100.0));
    List<PortfolioHolding> portB = List.of(new PortfolioHolding("qqq", 100.0));
    String payload = objectMapper.writeValueAsString(
        new PortfolioCompareRequest(portA, portB, 10_000.0, 5, "A", "B"));
    mockMvc.perform(post("/api/portfolio/compare")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isOk());
  }

  @Test
  void portfolioCompareReturns400WhenPortfolioAIsEmpty() throws Exception {
    List<PortfolioHolding> portB = List.of(new PortfolioHolding("qqq", 100.0));
    String payload = objectMapper.writeValueAsString(
        new PortfolioCompareRequest(List.of(), portB, 10_000.0, 5, "A", "B"));
    mockMvc.perform(post("/api/portfolio/compare")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isBadRequest());
  }

  @Test
  void aiPortfolioReturns200ForValidPrompt() throws Exception {
    String payload = objectMapper.writeValueAsString(new PortfolioRequest("aggressive growth"));
    mockMvc.perform(post("/api/ai/portfolio")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isOk());
  }

  @Test
  void aiPortfolioReturns400ForBlankPrompt() throws Exception {
    String payload = objectMapper.writeValueAsString(new PortfolioRequest(""));
    mockMvc.perform(post("/api/ai/portfolio")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isBadRequest());
  }

  @Test
  void aiPortfolioResponseHasFourAllocations() throws Exception {
    String payload = objectMapper.writeValueAsString(new PortfolioRequest("aggressive long term"));
    MvcResult result = mockMvc.perform(post("/api/ai/portfolio")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andReturn();
    PortfolioResponse response = objectMapper.readValue(
        result.getResponse().getContentAsString(), PortfolioResponse.class);
    assertEquals(4, response.allocation().size());
  }
}