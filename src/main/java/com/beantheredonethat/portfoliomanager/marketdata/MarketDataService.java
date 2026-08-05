package com.beantheredonethat.portfoliomanager.marketdata;

import com.beantheredonethat.portfoliomanager.exception.MarketDataException;

import java.util.Set;

/**
 * Provider-agnostic interface for fetching current market prices/NAVs.
 */
public interface MarketDataService {

    /**
     * Which asset types this implementation supports.
     */
    Set<AssetType> supportedAssetTypes();

    /**
     * Fetch current price information for the given request.
     * Implementations should throw MarketDataException on failures.
     */
    MarketDataResponse getCurrentPrice(MarketDataRequest request) throws MarketDataException;
}

