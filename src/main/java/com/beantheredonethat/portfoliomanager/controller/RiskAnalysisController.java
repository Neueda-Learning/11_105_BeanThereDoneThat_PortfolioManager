package com.beantheredonethat.portfoliomanager.controller;

import com.beantheredonethat.portfoliomanager.dto.RiskAnalysisResponse;
import com.beantheredonethat.portfoliomanager.service.RiskAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RiskAnalysisController {

    private final RiskAnalysisService riskAnalysisService;

    public RiskAnalysisController(RiskAnalysisService riskAnalysisService) {
        this.riskAnalysisService = riskAnalysisService;
    }

    @Operation(summary = "Analyze risk metrics for a stock symbol")
    @GetMapping("/api/risk-analysis/stock/{symbol}")
    public ResponseEntity<RiskAnalysisResponse> analyzeStock(@PathVariable String symbol) {
        return ResponseEntity.ok(riskAnalysisService.analyzeStock(symbol));
    }

    @Operation(summary = "Analyze risk metrics for a portfolio")
    @GetMapping("/api/risk-analysis/portfolio/{portfolioId}")
    public ResponseEntity<RiskAnalysisResponse> analyzePortfolio(@PathVariable Integer portfolioId) {
        return ResponseEntity.ok(riskAnalysisService.analyzePortfolio(portfolioId));
    }
}