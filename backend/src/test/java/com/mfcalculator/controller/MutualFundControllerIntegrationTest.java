package com.mfcalculator.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mfcalculator.dto.InvestmentRequest;
import com.mfcalculator.dto.InvestmentResponse;
import com.mfcalculator.service.CompareService;
import com.mfcalculator.service.FinanceService;
import com.mfcalculator.service.FundCatalogService;
import com.mfcalculator.service.MonteCarloService;
import com.mfcalculator.service.PortfolioService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MutualFundControllerIntegrationTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void calculateUsesCapmProviders() throws Exception {
    FundCatalogService fundCatalogService = new FundCatalogService();
    FinanceService financeService = new FinanceService(
        ticker -> 1.2,
        fundCatalogService,
        () -> 0.02,
        ticker -> 0.08
    );

    CompareService compareService = new CompareService(financeService, fundCatalogService);
    MonteCarloService monteCarloService = new MonteCarloService(financeService, ticker -> 1.2, fundCatalogService);

    MutualFundController controller = new MutualFundController(
        financeService,
        new PortfolioService(),
        fundCatalogService,
        compareService,
        monteCarloService,
        () -> 0.02,
        () -> 0.08
    );

    MockMvc mockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setMessageConverters(new MappingJackson2HttpMessageConverter())
        .build();

    InvestmentRequest request = new InvestmentRequest("vanguard-500", 1000.0, 1);
    String payload = objectMapper.writeValueAsString(request);

    String responseBody = mockMvc.perform(
            post("/api/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    InvestmentResponse body = objectMapper.readValue(responseBody, InvestmentResponse.class);
    assertNotNull(body);

    double expectedAnnualReturn = 0.02 + 1.2 * (0.08 - 0.02);
    double expectedFutureValue = 1000.0 * Math.exp(expectedAnnualReturn);

    assertEquals(expectedAnnualReturn, body.annualReturn(), 1e-9);
    assertEquals(expectedFutureValue, body.futureValue(), 1e-6);
    assertEquals(expectedFutureValue - 1000.0, body.gain(), 1e-6);
  }
}
