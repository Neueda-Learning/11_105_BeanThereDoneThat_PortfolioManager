package com.beantheredonethat.portfoliomanager.service;

import com.beantheredonethat.portfoliomanager.exception.YahooFinanceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

@Service
public class CurrencyConversionService {

    private static final Logger logger =
            LoggerFactory.getLogger(CurrencyConversionService.class);

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "SGD", "AED",
            "NZD", "HKD", "CNY", "SEK", "NOK", "DKK", "ZAR", "MXN", "BRL", "SAR"
    );

    private final YahooFinanceService yahooFinanceService;

    public CurrencyConversionService(YahooFinanceService yahooFinanceService) {
        this.yahooFinanceService = yahooFinanceService;
    }

    public BigDecimal convert(
            BigDecimal amount,
            String fromCurrency,
            String toCurrency) {

        if(amount == null
                || fromCurrency == null
                || toCurrency == null
                || fromCurrency.equalsIgnoreCase(toCurrency)) {

            return amount;
        }

        try {
            BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);
            return amount.multiply(rate)
                    .setScale(2, RoundingMode.HALF_UP);
        } catch(Exception ex) {
            logger.warn("Falling back to original amount; conversion failed from {} to {}: {}",
                    fromCurrency,
                    toCurrency,
                    ex.getMessage());
            return amount;
        }
    }



    public BigDecimal getExchangeRate(
            String fromCurrency,
            String toCurrency) {

        String normalizedFrom = normalizeCurrency(fromCurrency);
        String normalizedTo = normalizeCurrency(toCurrency);

        validateSupportedCurrency(normalizedFrom);
        validateSupportedCurrency(normalizedTo);

        logger.info("Exchange rate request: from={} to={}", normalizedFrom, normalizedTo);

        if(normalizedFrom.equals(normalizedTo)) {
            return BigDecimal.ONE.setScale(8, RoundingMode.HALF_UP);
        }

        String ticker = buildTicker(normalizedFrom, normalizedTo);
        logger.info("Generated Yahoo FX ticker: {}", ticker);

        try {
            BigDecimal rate = yahooFinanceService.getCurrentPrice(ticker)
                    .setScale(8, RoundingMode.HALF_UP);

            logger.info("Resolved exchange rate {} -> {} using {}: {}",
                    normalizedFrom,
                    normalizedTo,
                    ticker,
                    rate);
            return rate;
        } catch(IllegalArgumentException ex) {
            logger.error("Yahoo rejected FX ticker {} for {} -> {}: {}",
                    ticker,
                    normalizedFrom,
                    normalizedTo,
                    ex.getMessage());
            throw new IllegalArgumentException(
                    "Unable to fetch exchange rate for " + normalizedFrom + " to " + normalizedTo,
                    ex);
        } catch(YahooFinanceException ex) {
            logger.error("Yahoo FX lookup failed for {} -> {} using {}: {}",
                    normalizedFrom,
                    normalizedTo,
                    ticker,
                    ex.getMessage(),
                    ex);
            throw new YahooFinanceException(
                    "Unable to fetch exchange rate for " + normalizedFrom + " to " + normalizedTo,
                    ex);
        }
    }



    private String normalizeCurrency(String currency) {
        if(currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
        return currency.trim().toUpperCase();
    }



    private void validateSupportedCurrency(String currency) {
        if(!currency.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException(
                    "Invalid currency code '" + currency + "'. Use 3-letter ISO currency codes.");
        }

        if(!SUPPORTED_CURRENCIES.contains(currency)) {
            throw new IllegalArgumentException(
                    "Unsupported currency code '" + currency + "'. Supported currencies: "
                            + String.join(", ", SUPPORTED_CURRENCIES));
        }
    }



    private String buildTicker(String fromCurrency, String toCurrency) {
        return fromCurrency.toUpperCase() + toCurrency.toUpperCase() + "=X";
    }
}