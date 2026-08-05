package com.beantheredonethat.portfoliomanager.controller;

import com.beantheredonethat.portfoliomanager.dto.CreateTransactionRequest;
import com.beantheredonethat.portfoliomanager.dto.ImportSummaryResponse;
import com.beantheredonethat.portfoliomanager.dto.TransactionResponse;
import com.beantheredonethat.portfoliomanager.service.CustomerService;
import com.beantheredonethat.portfoliomanager.service.InvestmentTransactionService;
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
public class InvestmentTransactionController {

    private final InvestmentTransactionService transactionService;
        private final CustomerService customerService;

    public InvestmentTransactionController(
                        InvestmentTransactionService transactionService,
                        CustomerService customerService) {

        this.transactionService = transactionService;
                this.customerService = customerService;
    }

    @Operation(summary = "Create a transaction")
    @PostMapping("/api/transactions")
    public ResponseEntity<TransactionResponse> createTransaction(
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication,
            @Valid @RequestBody CreateTransactionRequest request) {

        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);

        TransactionResponse response =
                transactionService.createTransaction(customerId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Get all transactions")
    @GetMapping("/api/transactions")
    public ResponseEntity<List<TransactionResponse>>
        getAllTransactions(
                        @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
                        Authentication authentication) {

                Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);

        return ResponseEntity.ok(
                                transactionService.getAllTransactions(customerId));
    }

    @Operation(summary = "Export all transactions for the currently logged-in customer")
    @GetMapping("/api/transactions/export")
    public ResponseEntity<byte[]> exportTransactions(
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {

        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        String csv = transactionService.exportTransactionsCsv(customerId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"transactions-customer-" + customerId + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @Operation(summary = "Download the transaction import template")
    @GetMapping("/api/transactions/import-template")
    public ResponseEntity<byte[]> downloadTransactionImportTemplate() {
        String csv = transactionService.exportTransactionsTemplateCsv();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"transaction-import-template.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @Operation(summary = "Import transactions for the currently logged-in customer")
    @PostMapping(value = "/api/transactions/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportSummaryResponse> importTransactions(
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {

        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        return ResponseEntity.ok(transactionService.importTransactions(customerId, file));
    }

    @Operation(summary = "Get transaction by ID")
    @GetMapping("/api/transactions/{id}")
    public ResponseEntity<TransactionResponse>
        getTransactionById(
                        @PathVariable Integer id,
                        @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
                        Authentication authentication) {

                Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);

        return ResponseEntity.ok(
                                transactionService.getTransactionById(id, customerId));
    }

    @Operation(summary = "Get transactions by investment")
    @GetMapping("/api/investments/{investmentId}/transactions")
    public ResponseEntity<List<TransactionResponse>>
    getTransactionsByInvestment(
            @PathVariable Integer investmentId,
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {

        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);

        return ResponseEntity.ok(
                transactionService
                        .getTransactionsByInvestment(
                                investmentId,
                                customerId));
    }

    @Operation(summary = "Delete transaction")
    @DeleteMapping("/api/transactions/{id}")
    public ResponseEntity<Void>
        deleteTransaction(
                        @PathVariable Integer id,
                        @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
                        Authentication authentication) {

                Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);

                transactionService.deleteTransaction(id, customerId);

        return ResponseEntity.noContent().build();
    }
}