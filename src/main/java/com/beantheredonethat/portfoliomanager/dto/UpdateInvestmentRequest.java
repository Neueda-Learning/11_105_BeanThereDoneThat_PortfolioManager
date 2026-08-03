package com.beantheredonethat.portfoliomanager.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateInvestmentRequest {

    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be positive")
    private BigDecimal quantity;

    @DecimalMin(value = "0.0", inclusive = false, message = "Purchase price must be positive")
    private BigDecimal purchasePrice;

    private LocalDate purchaseDate;

    private String assetType;

    private String customAssetType;

    public UpdateInvestmentRequest() {
    }

    public UpdateInvestmentRequest(BigDecimal quantity, BigDecimal purchasePrice, LocalDate purchaseDate, String assetType, String customAssetType) {
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.purchaseDate = purchaseDate;
        this.assetType = assetType;
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
}

