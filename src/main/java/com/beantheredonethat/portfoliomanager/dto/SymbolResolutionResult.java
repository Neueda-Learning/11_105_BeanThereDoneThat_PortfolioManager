package com.beantheredonethat.portfoliomanager.dto;

public class SymbolResolutionResult {

    private String resolvedSymbol;
    private String exchange;
    private String currency;
    private String assetName;

    public SymbolResolutionResult() {
    }

    public SymbolResolutionResult(String resolvedSymbol, String exchange, String currency, String assetName) {
        this.resolvedSymbol = resolvedSymbol;
        this.exchange = exchange;
        this.currency = currency;
        this.assetName = assetName;
    }

    public String getResolvedSymbol() {
        return resolvedSymbol;
    }

    public void setResolvedSymbol(String resolvedSymbol) {
        this.resolvedSymbol = resolvedSymbol;
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

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }
}