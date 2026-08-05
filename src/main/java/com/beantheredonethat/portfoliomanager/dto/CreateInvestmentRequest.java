package com.beantheredonethat.portfoliomanager.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateInvestmentRequest {

    @NotNull(message = "Portfolio ID is required")
    private Integer portfolioId;

    private String symbol;
    private String schemeCode;

    private String companyName;

    private String assetType;

    private String customAssetType;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be positive")
    private BigDecimal quantity;

    @NotNull(message = "Purchase price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Purchase price must be positive")
    private BigDecimal purchasePrice;

    @NotNull(message = "Purchase date is required")
    private LocalDate purchaseDate;

    @DecimalMin(value = "0.0", inclusive = false, message = "Current price must be positive")
    private BigDecimal currentPrice;

    public CreateInvestmentRequest() {
    }
    public CreateInvestmentRequest(Integer portfolioId, String symbol,String schemeCode, String companyName, String assetType, String customAssetType, BigDecimal quantity, BigDecimal purchasePrice, LocalDate purchaseDate, BigDecimal currentPrice) {
        this.portfolioId = portfolioId;
        this.symbol = symbol;
        this.schemeCode = schemeCode;
        this.companyName = companyName;
        this.assetType = assetType;
        this.customAssetType = customAssetType;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.purchaseDate = purchaseDate;
        this.currentPrice = currentPrice;
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

    public String getSchemeCode() {
        return schemeCode;
    }

    public void setSchemeCode(String schemeCode) {
        this.schemeCode = schemeCode;
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

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }
}

