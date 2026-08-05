package com.beantheredonethat.portfoliomanager.controller;

import com.beantheredonethat.portfoliomanager.dto.CreatePortfolioRequest;
import com.beantheredonethat.portfoliomanager.dto.PortfolioResponse;
import com.beantheredonethat.portfoliomanager.dto.UpdatePortfolioRequest;
import com.beantheredonethat.portfoliomanager.service.CustomerService;
import com.beantheredonethat.portfoliomanager.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final CustomerService customerService;

    public PortfolioController(PortfolioService portfolioService, CustomerService customerService) {
        this.portfolioService = portfolioService;
        this.customerService = customerService;
    }

    @Operation(summary = "Create a new portfolio")
    @PostMapping
    public ResponseEntity<PortfolioResponse> createPortfolio(
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication,
            @Valid @RequestBody CreatePortfolioRequest request) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(portfolioService.createPortfolio(customerId, request));
    }

    @Operation(summary = "Get all portfolios")
    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> getAllPortfolios(
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        return ResponseEntity.ok(portfolioService.getAllPortfolios(customerId));
    }

    @Operation(summary = "Get portfolio by ID")
    @GetMapping("/{id}")
    public ResponseEntity<PortfolioResponse> getPortfolioById(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        return ResponseEntity.ok(portfolioService.getPortfolioById(id, customerId));
    }

    @Operation(summary = "Update portfolio by ID")
    @PutMapping("/{id}")
    public ResponseEntity<PortfolioResponse> updatePortfolio(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication,
            @Valid @RequestBody UpdatePortfolioRequest request) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        return ResponseEntity.ok(portfolioService.updatePortfolio(id, customerId, request));
    }

    @Operation(summary = "Delete portfolio by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePortfolio(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        portfolioService.deletePortfolio(id, customerId);
        return ResponseEntity.noContent().build();
    }
}
