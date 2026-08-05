package com.beantheredonethat.portfoliomanager.controller;

import com.beantheredonethat.portfoliomanager.dto.CreateInvestmentRequest;
import com.beantheredonethat.portfoliomanager.dto.ExchangeRateResponse;
import com.beantheredonethat.portfoliomanager.dto.ImportSummaryResponse;
import com.beantheredonethat.portfoliomanager.dto.InvestmentResponse;
import com.beantheredonethat.portfoliomanager.dto.UpdateInvestmentRequest;
import com.beantheredonethat.portfoliomanager.service.CustomerService;
import com.beantheredonethat.portfoliomanager.service.InvestmentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
public class InvestmentController {

    private final InvestmentService investmentService;
    private final CustomerService customerService;

    public InvestmentController(InvestmentService investmentService, CustomerService customerService) {
        this.investmentService = investmentService;
        this.customerService = customerService;
    }

    @Operation(summary = "Create a new investment")
    @PostMapping("/api/investments")
    public ResponseEntity<InvestmentResponse> createInvestment(
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication,
            @Valid @RequestBody CreateInvestmentRequest request) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        InvestmentResponse response = investmentService.createInvestment(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get all investments")
    @GetMapping("/api/investments")
    public ResponseEntity<List<InvestmentResponse>> getAllInvestments(
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        return ResponseEntity.ok(investmentService.getAllInvestments(customerId));
    }

        @Operation(summary = "Export all investments for the currently logged-in customer")
        @GetMapping("/api/investments/export")
        public ResponseEntity<byte[]> exportInvestments(
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        String csv = investmentService.exportInvestmentsCsv(customerId);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"investments-customer-" + customerId + ".csv\"")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(csv.getBytes(StandardCharsets.UTF_8));
        }

        @Operation(summary = "Download the investment import template")
        @GetMapping("/api/investments/import-template")
        public ResponseEntity<byte[]> downloadInvestmentImportTemplate() {
        String csv = investmentService.exportInvestmentsTemplateCsv();

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"investment-import-template.csv\"")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(csv.getBytes(StandardCharsets.UTF_8));
        }

        @Operation(summary = "Import investments for the currently logged-in customer")
        @PostMapping(value = "/api/investments/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ImportSummaryResponse> importInvestments(
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        return ResponseEntity.ok(investmentService.importInvestments(customerId, file));
        }

    @Operation(summary = "Get investment by ID")
    @GetMapping("/api/investments/{id}")
    public ResponseEntity<InvestmentResponse> getInvestmentById(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        return ResponseEntity.ok(investmentService.getInvestmentById(id, customerId));
    }

    @Operation(summary = "Get investments by portfolio")
    @GetMapping("/api/portfolios/{portfolioId}/investments")
    public ResponseEntity<List<InvestmentResponse>> getInvestmentsByPortfolio(
            @PathVariable Integer portfolioId,
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        return ResponseEntity.ok(investmentService.getInvestmentsByPortfolio(portfolioId, customerId));
    }

    @Operation(summary = "Update an investment")
    @PutMapping("/api/investments/{id}")
    public ResponseEntity<InvestmentResponse> updateInvestment(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication,
            @Valid @RequestBody UpdateInvestmentRequest request) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        return ResponseEntity.ok(investmentService.updateInvestment(id, customerId, request));
    }

    @Operation(summary = "Delete an investment")
    @DeleteMapping("/api/investments/{id}")
    public ResponseEntity<Void> deleteInvestment(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        investmentService.deleteInvestment(id, customerId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get currency exchange rate")
    @GetMapping("/api/investments/exchange-rate")
    public ResponseEntity<ExchangeRateResponse> getExchangeRate(
            @RequestParam String from,
            @RequestParam String to) {

        return ResponseEntity.ok(investmentService.getExchangeRate(from, to));
    }
}

