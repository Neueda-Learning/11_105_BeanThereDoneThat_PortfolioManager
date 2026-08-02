package com.beantheredonethat.portfoliomanager.controller;

import com.beantheredonethat.portfoliomanager.dto.LoginRequest;
import com.beantheredonethat.portfoliomanager.dto.LoginResponse;
import com.beantheredonethat.portfoliomanager.dto.RegisterRequest;
import com.beantheredonethat.portfoliomanager.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Operation(summary = "Register Customer")
    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterRequest request) {
        return customerService.register(request);
    }

    @Operation(summary = "Customer Login")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return customerService.login(request);
    }
}