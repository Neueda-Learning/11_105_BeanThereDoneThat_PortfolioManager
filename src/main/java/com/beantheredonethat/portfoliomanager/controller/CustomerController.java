package com.beantheredonethat.portfoliomanager.controller;

import com.beantheredonethat.portfoliomanager.dto.AccountSummaryResponse;
import com.beantheredonethat.portfoliomanager.dto.ChangePasswordRequest;
import com.beantheredonethat.portfoliomanager.dto.CustomerResponse;
import com.beantheredonethat.portfoliomanager.dto.LoginRequest;
import com.beantheredonethat.portfoliomanager.dto.LoginResponse;
import com.beantheredonethat.portfoliomanager.dto.PortfolioResponse;
import com.beantheredonethat.portfoliomanager.dto.RegisterRequest;
import com.beantheredonethat.portfoliomanager.dto.UpdateCustomerRequest;
import com.beantheredonethat.portfoliomanager.service.CustomerService;
import com.beantheredonethat.portfoliomanager.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final PortfolioService portfolioService;

    public CustomerController(CustomerService customerService, PortfolioService portfolioService) {
        this.customerService = customerService;
        this.portfolioService = portfolioService;
    }

    @Operation(summary = "Register a new customer")
    @PostMapping("/register")
    public ResponseEntity<CustomerResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerService.register(request));
    }

    @Operation(summary = "Customer login")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(customerService.login(request));
    }

    @Operation(summary = "Get customer by ID")
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        if (!customerId.equals(id)) {
            throw new SecurityException("Access denied for requested customer ID.");
        }
        return ResponseEntity.ok(customerService.getCustomerById(customerId));
    }

    @Operation(summary = "Get currently logged-in customer profile")
    @GetMapping("/profile")
    public ResponseEntity<CustomerResponse> getCurrentCustomerProfile(
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        return ResponseEntity.ok(customerService.getCustomerById(customerId));
    }

    @Operation(summary = "Update currently logged-in customer profile")
    @PutMapping("/profile")
    public ResponseEntity<CustomerResponse> updateCurrentCustomerProfile(
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication,
            @Valid @RequestBody UpdateCustomerRequest request) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        return ResponseEntity.ok(customerService.updateProfile(customerId, request));
    }

    @Operation(summary = "Change password for currently logged-in customer")
    @PutMapping("/profile/password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        customerService.changePassword(customerId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get account summary counts for currently logged-in customer")
    @GetMapping("/profile/summary")
    public ResponseEntity<AccountSummaryResponse> getProfileSummary(
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        return ResponseEntity.ok(customerService.getAccountSummary(customerId));
    }

    @Operation(summary = "Export currently logged-in customer data as CSV")
    @GetMapping("/profile/export")
    public ResponseEntity<byte[]> exportProfileData(
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        String csv = customerService.exportCustomerDataCsv(customerId);
        String fileName = "portfolio-data-customer-" + customerId + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @Operation(summary = "Delete currently logged-in customer account")
    @DeleteMapping("/profile")
    public ResponseEntity<Void> deleteCurrentCustomerProfile(
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        customerService.deleteCustomerProfile(customerId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all customers")
    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers(
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        return ResponseEntity.ok(List.of(customerService.getCustomerById(customerId)));
    }

    @Operation(summary = "Update customer by ID")
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication,
            @Valid @RequestBody UpdateCustomerRequest request) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        if (!customerId.equals(id)) {
            throw new SecurityException("Access denied for requested customer ID.");
        }
        return ResponseEntity.ok(customerService.updateCustomer(customerId, request));
    }

    @Operation(summary = "Delete customer by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        if (!customerId.equals(id)) {
            throw new SecurityException("Access denied for requested customer ID.");
        }
        customerService.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all portfolios for a customer")
    @GetMapping("/{customerId}/portfolios")
    public ResponseEntity<List<PortfolioResponse>> getPortfoliosByCustomer(
            @PathVariable Integer customerId,
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {
        Integer resolvedCustomerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        if (!resolvedCustomerId.equals(customerId)) {
            throw new SecurityException("Access denied for requested customer ID.");
        }
        return ResponseEntity.ok(portfolioService.getAllPortfolios(resolvedCustomerId));
    }
}