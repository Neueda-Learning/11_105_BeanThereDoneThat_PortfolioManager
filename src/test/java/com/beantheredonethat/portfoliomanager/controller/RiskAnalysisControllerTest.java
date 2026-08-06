package com.beantheredonethat.portfoliomanager.controller;

import com.beantheredonethat.portfoliomanager.dto.RiskAnalysisResponse;
import com.beantheredonethat.portfoliomanager.dto.SymbolResolutionResult;
import com.beantheredonethat.portfoliomanager.exception.PortfolioNotFoundException;
import com.beantheredonethat.portfoliomanager.service.CustomerService;
import com.beantheredonethat.portfoliomanager.service.RiskAnalysisService;
import com.beantheredonethat.portfoliomanager.service.SymbolResolverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RiskAnalysisController.class)
@AutoConfigureMockMvc(addFilters = false)
class RiskAnalysisControllerTest {

    @Autowired
        private WebApplicationContext webApplicationContext;

        private MockMvc mockMvc;

    @MockBean
    private RiskAnalysisService riskAnalysisService;

    @MockBean
    private SymbolResolverService symbolResolverService;

    @MockBean
    private CustomerService customerService;

        @BeforeEach
        void setUpMockMvc() {
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        }

    @Test
    void analyzeStock_success_returnsJsonAndResolvedMetadata() throws Exception {
        when(symbolResolverService.resolveSymbol("aapl", "STOCK"))
                .thenReturn(new SymbolResolutionResult("AAPL", "NASDAQ", "USD", "Apple Inc"));

        RiskAnalysisResponse response = new RiskAnalysisResponse();
        response.setAnnualizedVolatility(new BigDecimal("23.45"));
        response.setMaximumDrawdown(new BigDecimal("-12.34"));
        response.setAverageAnnualReturn(new BigDecimal("15.67"));
        response.setSharpeRatio(new BigDecimal("1.2345"));
        response.setRiskLevel("MODERATE");

        when(riskAnalysisService.analyzeStock("AAPL")).thenReturn(response);

        mockMvc.perform(get("/api/risk-analysis/stock/aapl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.exchange").value("NASDAQ"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.riskLevel").value("MODERATE"));

        verify(symbolResolverService).resolveSymbol("aapl", "STOCK");
        verify(riskAnalysisService).analyzeStock("AAPL");
    }

    @Test
    void analyzeStock_fallbackToPathVariableWhenResolverReturnsNullSymbol() throws Exception {
        when(symbolResolverService.resolveSymbol(" msft ", "STOCK"))
                .thenReturn(new SymbolResolutionResult(null, null, null, null));

        RiskAnalysisResponse response = new RiskAnalysisResponse();
        response.setRiskLevel("HIGH");
        when(riskAnalysisService.analyzeStock("MSFT")).thenReturn(response);

        mockMvc.perform(get("/api/risk-analysis/stock/ msft "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("MSFT"));
    }

    @Test
    void analyzePortfolio_success_returnsJson() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(9))).thenReturn(9);

        RiskAnalysisResponse response = new RiskAnalysisResponse();
        response.setPortfolioId(100);
        response.setPortfolioValue(new BigDecimal("5000.00"));
        response.setRiskLevel("LOW");

        when(riskAnalysisService.analyzePortfolio(100, 9)).thenReturn(response);

        mockMvc.perform(get("/api/risk-analysis/portfolio/100").header("X-Customer-Id", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value(100))
                .andExpect(jsonPath("$.portfolioValue").value(5000.00))
                .andExpect(jsonPath("$.riskLevel").value("LOW"));

        verify(customerService).resolveCustomerId(any(), eq(9));
        verify(riskAnalysisService).analyzePortfolio(100, 9);
    }

    @Test
    void analyzePortfolio_notFound_returns404() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(9))).thenReturn(9);
        when(riskAnalysisService.analyzePortfolio(999, 9)).thenThrow(new PortfolioNotFoundException("Portfolio not found"));

        mockMvc.perform(get("/api/risk-analysis/portfolio/999").header("X-Customer-Id", "9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Portfolio not found"));
    }

    @Test
    void analyzePortfolio_invalidInput_returns400() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(9))).thenReturn(9);
        when(riskAnalysisService.analyzePortfolio(100, 9)).thenThrow(new IllegalArgumentException("No investments"));

        mockMvc.perform(get("/api/risk-analysis/portfolio/100").header("X-Customer-Id", "9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No investments"));
    }
}
