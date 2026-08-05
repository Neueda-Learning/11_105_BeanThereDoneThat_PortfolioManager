package com.beantheredonethat.portfoliomanager.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Investment {

    private Integer investmentId;
    private Integer portfolioId;
    private String symbol;
    private String companyName;
    private String exchange;
    private String currency;
    private String assetType;
    private String customAssetType;
    private BigDecimal quantity;
    private BigDecimal investedAmount;
    private BigDecimal purchasePrice;
    private BigDecimal currentPrice;
    private BigDecimal currentValue;
    private BigDecimal profitLoss;
    private LocalDate purchaseDate;

    public Investment() {
    }

    public Investment(Integer investmentId, Integer portfolioId, String symbol, String companyName, String exchange, String currency, String assetType, String customAssetType, BigDecimal quantity, BigDecimal investedAmount, BigDecimal purchasePrice, BigDecimal currentPrice, BigDecimal currentValue, BigDecimal profitLoss, LocalDate purchaseDate) {
        this.investmentId = investmentId;
        this.portfolioId = portfolioId;
        this.symbol = symbol;
        this.companyName = companyName;
        this.exchange = exchange;
        this.currency = currency;
        this.assetType = assetType;
        this.customAssetType = customAssetType;
        this.quantity = quantity;
        this.investedAmount = investedAmount;
        this.purchasePrice = purchasePrice;
        this.currentPrice = currentPrice;
        this.currentValue = currentValue;
        this.profitLoss = profitLoss;
        this.purchaseDate = purchaseDate;
    }

    public Investment(Integer portfolioId, String symbol, String companyName, String exchange, String currency, String assetType, String customAssetType, BigDecimal quantity, BigDecimal investedAmount, BigDecimal purchasePrice, BigDecimal currentPrice, BigDecimal currentValue, BigDecimal profitLoss, LocalDate purchaseDate) {
        this(null, portfolioId, symbol, companyName, exchange, currency, assetType, customAssetType, quantity, investedAmount, purchasePrice, currentPrice, currentValue, profitLoss, purchaseDate);
    }

    public Integer getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(Integer investmentId) {
        this.investmentId = investmentId;
    }

    public Integer getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(Integer portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getCustomAssetType() {
        return customAssetType;
    }

    public void setCustomAssetType(String customAssetType) {
        this.customAssetType = customAssetType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getInvestedAmount() {
        return investedAmount;
    }

    public void setInvestedAmount(BigDecimal investedAmount) {
        this.investedAmount = investedAmount;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }

    public BigDecimal getProfitLoss() {
        return profitLoss;
    }

    public void setProfitLoss(BigDecimal profitLoss) {
        this.profitLoss = profitLoss;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }
}

