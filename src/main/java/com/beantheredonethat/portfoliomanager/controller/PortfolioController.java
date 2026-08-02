package com.beantheredonethat.portfoliomanager.controller;

import com.beantheredonethat.portfoliomanager.dto.CreatePortfolioRequest;
import com.beantheredonethat.portfoliomanager.dto.PortfolioResponse;
import com.beantheredonethat.portfoliomanager.dto.UpdatePortfolioRequest;
import com.beantheredonethat.portfoliomanager.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @Operation(summary = "Create a new portfolio")
    @PostMapping
    public ResponseEntity<PortfolioResponse> createPortfolio(
            @Valid @RequestBody CreatePortfolioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portfolioService.createPortfolio(request));
    }

    @Operation(summary = "Get all portfolios")
    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> getAllPortfolios() {
        return ResponseEntity.ok(portfolioService.getAllPortfolios());
    }

    @Operation(summary = "Get portfolio by ID")
    @GetMapping("/{id}")
    public ResponseEntity<PortfolioResponse> getPortfolioById(@PathVariable Integer id) {
        return ResponseEntity.ok(portfolioService.getPortfolioById(id));
    }

    @Operation(summary = "Update portfolio by ID")
    @PutMapping("/{id}")
    public ResponseEntity<PortfolioResponse> updatePortfolio(
            @PathVariable Integer id,
            @Valid @RequestBody UpdatePortfolioRequest request) {
        return ResponseEntity.ok(portfolioService.updatePortfolio(id, request));
    }

    @Operation(summary = "Delete portfolio by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePortfolio(@PathVariable Integer id) {
        portfolioService.deletePortfolio(id);
        return ResponseEntity.noContent().build();
    }
}
