package com.beantheredonethat.portfoliomanager.dto;

import com.beantheredonethat.portfoliomanager.entity.Portfolio;

public class PortfolioResponse {

    private Integer portfolioId;
    private Integer customerId;
    private String portfolioName;

    public PortfolioResponse() {
    }

    public PortfolioResponse(Integer portfolioId, Integer customerId, String portfolioName) {
        this.portfolioId = portfolioId;
        this.customerId = customerId;
        this.portfolioName = portfolioName;
    }

    public PortfolioResponse(Portfolio portfolio) {
        this.portfolioId = portfolio.getPortfolioId();
        this.customerId = portfolio.getCustomer() != null ? portfolio.getCustomer().getCustomerId() : null;
        this.portfolioName = portfolio.getPortfolioName();
    }

    public Integer getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(Integer portfolioId) {
        this.portfolioId = portfolioId;
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
