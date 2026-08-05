package com.beantheredonethat.portfoliomanager.exception;

/**
 * Generic exception for market data provider failures.
 */
public class MarketDataException extends RuntimeException {
    public MarketDataException(String message) {
        super(message);
    }

    public MarketDataException(String message, Throwable cause) {
        super(message, cause);
    }
}

