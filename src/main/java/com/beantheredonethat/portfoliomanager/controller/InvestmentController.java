package com.beantheredonethat.portfoliomanager.controller;

import com.beantheredonethat.portfoliomanager.dto.CreateInvestmentRequest;
import com.beantheredonethat.portfoliomanager.dto.InvestmentResponse;
import com.beantheredonethat.portfoliomanager.dto.UpdateInvestmentRequest;
import com.beantheredonethat.portfoliomanager.service.InvestmentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class InvestmentController {

    private final InvestmentService investmentService;

    public InvestmentController(InvestmentService investmentService) {
        this.investmentService = investmentService;
    }

    @Operation(summary = "Create a new investment")
    @PostMapping("/api/investments")
    public ResponseEntity<InvestmentResponse> createInvestment(@Valid @RequestBody CreateInvestmentRequest request) {
        InvestmentResponse response = investmentService.createInvestment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get all investments")
    @GetMapping("/api/investments")
    public ResponseEntity<List<InvestmentResponse>> getAllInvestments() {
        return ResponseEntity.ok(investmentService.getAllInvestments());
    }

    @Operation(summary = "Get investment by ID")
    @GetMapping("/api/investments/{id}")
    public ResponseEntity<InvestmentResponse> getInvestmentById(@PathVariable Integer id) {
        return ResponseEntity.ok(investmentService.getInvestmentById(id));
    }

    @Operation(summary = "Get investments by portfolio")
    @GetMapping("/api/portfolios/{portfolioId}/investments")
    public ResponseEntity<List<InvestmentResponse>> getInvestmentsByPortfolio(@PathVariable Integer portfolioId) {
        return ResponseEntity.ok(investmentService.getInvestmentsByPortfolio(portfolioId));
    }

    @Operation(summary = "Update an investment")
    @PutMapping("/api/investments/{id}")
    public ResponseEntity<InvestmentResponse> updateInvestment(@PathVariable Integer id, @Valid @RequestBody UpdateInvestmentRequest request) {
        return ResponseEntity.ok(investmentService.updateInvestment(id, request));
    }

    @Operation(summary = "Delete an investment")
    @DeleteMapping("/api/investments/{id}")
    public ResponseEntity<Void> deleteInvestment(@PathVariable Integer id) {
        investmentService.deleteInvestment(id);
        return ResponseEntity.noContent().build();
    }
}

