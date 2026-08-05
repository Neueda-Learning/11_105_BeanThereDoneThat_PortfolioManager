package com.beantheredonethat.portfoliomanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreatePortfolioRequest {

    @NotBlank(message = "Portfolio name is required")
    @Size(max = 100, message = "Portfolio name must be at most 100 characters")
    private String portfolioName;

    public CreatePortfolioRequest() {
    }

    public CreatePortfolioRequest(String portfolioName) {
        this.portfolioName = portfolioName;
    }

    public String getPortfolioName() {
        return portfolioName;
    }

    public void setPortfolioName(String portfolioName) {
        this.portfolioName = portfolioName;
    }
}
