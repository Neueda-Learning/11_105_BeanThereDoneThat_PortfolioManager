package com.beantheredonethat.portfoliomanager.marketdata;

import java.util.Map;

/**
 * Request object for market data providers.
 */
public class MarketDataRequest {

    private AssetType assetType;

    private String symbol;
    // Stocks / ETFs / Crypto symbol

    private String schemeCode;
    // Mutual fund scheme code (AMFI)

    private String companyName;
    // Mutual fund name used to find schemeCode automatically

    private String assetIdentifier;
    // Provider-specific id (e.g., CoinGecko id)

    private String currency;
    // Optional currency

    private Map<String, String> metadata;
    // Extensible fields



    public MarketDataRequest() {
    }



    public MarketDataRequest(
            AssetType assetType,
            String symbol,
            String schemeCode,
            String companyName,
            String assetIdentifier,
            String currency,
            Map<String, String> metadata) {

        this.assetType = assetType;
        this.symbol = symbol;
        this.schemeCode = schemeCode;
        this.companyName = companyName;
        this.assetIdentifier = assetIdentifier;
        this.currency = currency;
        this.metadata = metadata;
    }



    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
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



    public String getAssetIdentifier() {
        return assetIdentifier;
    }

    public void setAssetIdentifier(String assetIdentifier) {
        this.assetIdentifier = assetIdentifier;
    }



    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }



    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}