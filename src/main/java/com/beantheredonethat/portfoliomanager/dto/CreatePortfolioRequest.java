package com.beantheredonethat.portfoliomanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreatePortfolioRequest {

    @NotNull(message = "Customer ID is required")
    private Integer customerId;

    @NotBlank(message = "Portfolio name is required")
    @Size(max = 100, message = "Portfolio name must be at most 100 characters")
    private String portfolioName;

    public CreatePortfolioRequest() {
    }

    public CreatePortfolioRequest(Integer customerId, String portfolioName) {
        this.customerId = customerId;
        this.portfolioName = portfolioName;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getPortfolioName() {
        return portfolioName;
    }

    public void setPortfolioName(String portfolioName) {
        this.portfolioName = portfolioName;
    }
}
