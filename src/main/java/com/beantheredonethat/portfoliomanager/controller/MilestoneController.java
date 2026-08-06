package com.beantheredonethat.portfoliomanager.controller;

import com.beantheredonethat.portfoliomanager.dto.CreateMilestoneRequest;
import com.beantheredonethat.portfoliomanager.dto.MilestoneResponse;
import com.beantheredonethat.portfoliomanager.dto.NextMilestoneResponse;
import com.beantheredonethat.portfoliomanager.dto.UpdateMilestoneOrderRequest;
import com.beantheredonethat.portfoliomanager.dto.UpdateMilestoneRequest;
import com.beantheredonethat.portfoliomanager.service.CustomerService;
import com.beantheredonethat.portfoliomanager.service.MilestoneService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/api/milestones")
public class MilestoneController {

    private final MilestoneService milestoneService;
    private final CustomerService customerService;

    public MilestoneController(MilestoneService milestoneService, CustomerService customerService) {
        this.milestoneService = milestoneService;
        this.customerService = customerService;
    }

    @Operation(summary = "Create a milestone for the currently logged-in customer")
    @PostMapping
    public ResponseEntity<MilestoneResponse> createMilestone(
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication,
            @Valid @RequestBody CreateMilestoneRequest request) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        MilestoneResponse response = milestoneService.createMilestone(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Fetch milestones for the currently logged-in customer")
    @GetMapping
    public ResponseEntity<List<MilestoneResponse>> getMilestones(
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        return ResponseEntity.ok(milestoneService.getMilestones(customerId));
    }

    @Operation(summary = "Get the next closest milestone for the currently logged-in customer")
    @GetMapping("/next")
    public ResponseEntity<NextMilestoneResponse> getNextMilestone(
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        return ResponseEntity.ok(milestoneService.getNextMilestone(customerId));
    }

    @Operation(summary = "Update milestone details for the currently logged-in customer")
    @PutMapping("/{id}")
    public ResponseEntity<MilestoneResponse> updateMilestone(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication,
            @Valid @RequestBody UpdateMilestoneRequest request) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        return ResponseEntity.ok(milestoneService.updateMilestone(customerId, id, request));
    }

    @Operation(summary = "Update milestone display order for the currently logged-in customer")
    @PutMapping("/order")
    public ResponseEntity<List<MilestoneResponse>> updateMilestoneOrder(
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication,
            @Valid @RequestBody UpdateMilestoneOrderRequest request) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        return ResponseEntity.ok(milestoneService.updateMilestoneOrder(customerId, request));
    }

    @Operation(summary = "Delete milestone for the currently logged-in customer")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMilestone(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Customer-Id", required = false) Integer headerCustomerId,
            Authentication authentication) {
        Integer customerId = customerService.resolveCustomerId(authentication, headerCustomerId);
        milestoneService.deleteMilestone(customerId, id);
        return ResponseEntity.noContent().build();
    }
}
