package com.beantheredonethat.portfoliomanager.dto;

import java.math.BigDecimal;

public class RiskAnalysisResponse {

    private String symbol;
    private Integer portfolioId;
    private BigDecimal portfolioValue;
    private BigDecimal annualizedVolatility;
    private BigDecimal maximumDrawdown;
    private BigDecimal averageAnnualReturn;
    private BigDecimal sharpeRatio;
    private String riskLevel;

    public RiskAnalysisResponse() {
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Integer getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(Integer portfolioId) {
        this.portfolioId = portfolioId;
    }

    public BigDecimal getPortfolioValue() {
        return portfolioValue;
    }

    public void setPortfolioValue(BigDecimal portfolioValue) {
        this.portfolioValue = portfolioValue;
    }

    public BigDecimal getAnnualizedVolatility() {
        return annualizedVolatility;
    }

    public void setAnnualizedVolatility(BigDecimal annualizedVolatility) {
        this.annualizedVolatility = annualizedVolatility;
    }

    public BigDecimal getMaximumDrawdown() {
        return maximumDrawdown;
    }

    public void setMaximumDrawdown(BigDecimal maximumDrawdown) {
        this.maximumDrawdown = maximumDrawdown;
    }

    public BigDecimal getAverageAnnualReturn() {
        return averageAnnualReturn;
    }

    public void setAverageAnnualReturn(BigDecimal averageAnnualReturn) {
        this.averageAnnualReturn = averageAnnualReturn;
    }

    public BigDecimal getSharpeRatio() {
        return sharpeRatio;
    }

    public void setSharpeRatio(BigDecimal sharpeRatio) {
        this.sharpeRatio = sharpeRatio;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
}