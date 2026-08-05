package com.beantheredonethat.portfoliomanager.marketdata;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response from a MarketDataService.
 */
public class MarketDataResponse {

    private BigDecimal price;
    private String currency;
    private Instant timestamp;
    private AssetType assetType;
    private String providerId;

    public MarketDataResponse() {
    }

    public MarketDataResponse(BigDecimal price, String currency, Instant timestamp, AssetType assetType, String providerId) {
        this.price = price;
        this.currency = currency;
        this.timestamp = timestamp;
        this.assetType = assetType;
        this.providerId = providerId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
}