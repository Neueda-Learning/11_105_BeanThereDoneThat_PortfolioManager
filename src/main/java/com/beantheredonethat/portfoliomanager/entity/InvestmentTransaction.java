package com.beantheredonethat.portfoliomanager.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InvestmentTransaction {

    private Integer transactionId;
    private Integer investmentId;

    private String symbol;
    private String companyName;
    private String assetType;

    private LocalDate transactionDate;
    private String transactionType;
    private BigDecimal quantity;
    private BigDecimal transactionPrice;
    private BigDecimal transactionAmount;

    public InvestmentTransaction() {
    }

    public InvestmentTransaction(
            Integer transactionId,
            Integer investmentId,
            String symbol,
            String companyName,
            String assetType,
            LocalDate transactionDate,
            String transactionType,
            BigDecimal quantity,
            BigDecimal transactionPrice,
            BigDecimal transactionAmount) {

        this.transactionId = transactionId;
        this.investmentId = investmentId;
        this.symbol = symbol;
        this.companyName = companyName;
        this.assetType = assetType;
        this.transactionDate = transactionDate;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.transactionPrice = transactionPrice;
        this.transactionAmount = transactionAmount;
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }

    public Integer getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(Integer investmentId) {
        this.investmentId = investmentId;
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

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTransactionPrice() {
        return transactionPrice;
    }

    public void setTransactionPrice(BigDecimal transactionPrice) {
        this.transactionPrice = transactionPrice;
    }

    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(BigDecimal transactionAmount) {
        this.transactionAmount = transactionAmount;
    }
}