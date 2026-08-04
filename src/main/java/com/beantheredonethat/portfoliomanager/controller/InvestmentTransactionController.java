package com.beantheredonethat.portfoliomanager.controller;

import com.beantheredonethat.portfoliomanager.dto.CreateTransactionRequest;
import com.beantheredonethat.portfoliomanager.dto.TransactionResponse;
import com.beantheredonethat.portfoliomanager.service.InvestmentTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class InvestmentTransactionController {

    private final InvestmentTransactionService transactionService;

    public InvestmentTransactionController(
            InvestmentTransactionService transactionService) {

        this.transactionService = transactionService;
    }

    @Operation(summary = "Create a transaction")
    @PostMapping("/api/transactions")
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request) {

        TransactionResponse response =
                transactionService.createTransaction(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Get all transactions")
    @GetMapping("/api/transactions")
    public ResponseEntity<List<TransactionResponse>>
    getAllTransactions() {

        return ResponseEntity.ok(
                transactionService.getAllTransactions());
    }

    @Operation(summary = "Get transaction by ID")
    @GetMapping("/api/transactions/{id}")
    public ResponseEntity<TransactionResponse>
    getTransactionById(@PathVariable Integer id) {

        return ResponseEntity.ok(
                transactionService.getTransactionById(id));
    }

    @Operation(summary = "Get transactions by investment")
    @GetMapping("/api/investments/{investmentId}/transactions")
    public ResponseEntity<List<TransactionResponse>>
    getTransactionsByInvestment(
            @PathVariable Integer investmentId) {

        return ResponseEntity.ok(
                transactionService
                        .getTransactionsByInvestment(
                                investmentId));
    }

    @Operation(summary = "Delete transaction")
    @DeleteMapping("/api/transactions/{id}")
    public ResponseEntity<Void>
    deleteTransaction(@PathVariable Integer id) {

        transactionService.deleteTransaction(id);

        return ResponseEntity.noContent().build();
    }
}