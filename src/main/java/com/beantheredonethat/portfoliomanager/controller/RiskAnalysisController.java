package com.beantheredonethat.portfoliomanager.controller;

import com.beantheredonethat.portfoliomanager.dto.RiskAnalysisResponse;
import com.beantheredonethat.portfoliomanager.dto.SymbolResolutionResult;
import com.beantheredonethat.portfoliomanager.service.CustomerService;
import com.beantheredonethat.portfoliomanager.service.RiskAnalysisService;
import com.beantheredonethat.portfoliomanager.service.SymbolResolverService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
public class RiskAnalysisController {

    private final RiskAnalysisService riskAnalysisService;
    private final SymbolResolverService symbolResolverService;
    private final CustomerService customerService;

    public RiskAnalysisController(
            RiskAnalysisService riskAnalysisService,
            SymbolResolverService symbolResolverService,
            CustomerService customerService) {
        this.riskAnalysisService = riskAnalysisService;
        this.symbolResolverService = symbolResolverService;
        this.customerService = customerService;
    }

    @Operation(summary = "Analyze risk metrics for a stock symbol")
    @GetMapping("/api/risk-analysis/stock/{symbol}")
    public ResponseEntity<RiskAnalysisResponse> analyzeStock(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "STOCK") String assetType) {

        SymbolResolutionResult resolution = symbolResolverService.resolveSymbol(symbol, assetType);
        String resolvedSymbol = resolution.getResolvedSymbol() != null
                ? resolution.getResolvedSymbol()
                : symbol.trim().toUpperCase(Locale.ROOT);

        RiskAnalysisResponse response = riskAnalysisService.analyzeStock(resolvedSymbol);
        response.setSymbol(resolvedSymbol);
        response.setExchange(resolution.getExchange());
        response.setCurrency(resolution.getCurrency());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Analyze risk metrics for a portfolio")
    @GetMapping("/api/risk-analysis/portfolio/{portfolioId}")
    public ResponseEntity<RiskAnalysisResponse> analyzePortfolio(
            @PathVariable Integer portfolioId,
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        return ResponseEntity.ok(riskAnalysisService.analyzePortfolio(portfolioId, customerId));
    }
}