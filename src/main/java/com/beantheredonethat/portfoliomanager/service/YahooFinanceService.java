package com.beantheredonethat.portfoliomanager.service;

import com.beantheredonethat.portfoliomanager.exception.YahooFinanceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class YahooFinanceService {

    private static final Logger logger = LoggerFactory.getLogger(YahooFinanceService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Retry settings to handle transient rate limits from Yahoo
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000L;

    public YahooFinanceService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public BigDecimal getCurrentPrice(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new YahooFinanceException("Invalid symbol");
        }

        String normalized = symbol.trim().toUpperCase();
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + normalized;

        JsonNode firstResult = fetchChartResult(url, normalized);
        JsonNode meta = firstResult.path("meta");
        if (meta.isMissingNode() || meta.isNull()) {
            throw new YahooFinanceException("Missing meta section in chart result for symbol: " + normalized);
        }

        JsonNode priceNode = meta.path("regularMarketPrice");
        if (priceNode.isMissingNode() || priceNode.isNull()) {
            throw new YahooFinanceException("Missing regularMarketPrice for symbol: " + normalized);
        }

        return BigDecimal.valueOf(priceNode.asDouble());
    }

    public List<HistoricalPricePoint> getHistoricalPrices(String symbol, String range) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticker symbol is required");
        }
        if (range == null || range.trim().isEmpty()) {
            throw new IllegalArgumentException("Historical range is required");
        }

        String normalized = symbol.trim().toUpperCase();
        String url = String.format(
                "https://query1.finance.yahoo.com/v8/finance/chart/%s?interval=1d&range=%s&includeAdjustedClose=true",
                normalized,
                range.trim());

        JsonNode firstResult = fetchChartResult(url, normalized);
        JsonNode timestamps = firstResult.path("timestamp");
        JsonNode indicators = firstResult.path("indicators");
        JsonNode quote = indicators.path("quote");
        JsonNode quoteSeries = quote.isArray() && quote.size() > 0 ? quote.get(0) : null;
        JsonNode closeSeries = quoteSeries == null ? null : quoteSeries.path("close");
        JsonNode adjustedClose = extractAdjustedCloseSeries(indicators);

        if (timestamps == null || !timestamps.isArray() || closeSeries == null || !closeSeries.isArray()) {
            throw new YahooFinanceException("Missing historical data for symbol: " + normalized);
        }

        int pointCount = Math.min(timestamps.size(), closeSeries.size());
        List<HistoricalPricePoint> historicalPrices = new ArrayList<>();
        for (int index = 0; index < pointCount; index++) {
            JsonNode closeNode = adjustedClose != null && index < adjustedClose.size() && !adjustedClose.get(index).isNull()
                    ? adjustedClose.get(index)
                    : closeSeries.get(index);

            if (closeNode == null || closeNode.isNull()) {
                continue;
            }

            double closePrice = closeNode.asDouble();
            if (closePrice <= 0.0d) {
                continue;
            }

            LocalDate tradingDate = Instant.ofEpochSecond(timestamps.get(index).asLong())
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate();
            historicalPrices.add(new HistoricalPricePoint(tradingDate, BigDecimal.valueOf(closePrice)));
        }

        if (historicalPrices.size() < 2) {
            throw new YahooFinanceException("Missing historical data for symbol: " + normalized);
        }

        return historicalPrices;
    }

    private JsonNode extractAdjustedCloseSeries(JsonNode indicators) {
        JsonNode adjustedCloseCollection = indicators.path("adjclose");
        if (!adjustedCloseCollection.isArray() || adjustedCloseCollection.size() == 0) {
            return null;
        }

        JsonNode adjustedCloseSeries = adjustedCloseCollection.get(0).path("adjclose");
        return adjustedCloseSeries.isArray() ? adjustedCloseSeries : null;
    }

    private JsonNode fetchChartResult(String url, String normalizedSymbol) {
        HttpEntity<Void> requestEntity = new HttpEntity<>(buildHeaders());

        int attempt = 0;
        long backoff = INITIAL_BACKOFF_MS;
        while (true) {
            attempt++;
            try {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
                String resp = response.getBody();
                if (resp == null) {
                    throw new YahooFinanceException("Empty response from Yahoo Finance for symbol: " + normalizedSymbol);
                }

                JsonNode root = objectMapper.readTree(resp);
                JsonNode chart = root.path("chart");
                if (chart.isMissingNode() || chart.isNull()) {
                    throw new YahooFinanceException("No chart data for symbol: " + normalizedSymbol);
                }

                JsonNode results = chart.path("result");
                if (!results.isArray() || results.size() == 0) {
                    JsonNode errorNode = chart.path("error");
                    if (!errorNode.isMissingNode() && !errorNode.isNull()) {
                        String description = errorNode.path("description").asText(errorNode.toString());
                        throw new IllegalArgumentException("Invalid ticker symbol: " + normalizedSymbol + ". " + description);
                    }
                    throw new IllegalArgumentException("Invalid ticker symbol: " + normalizedSymbol);
                }

                return results.get(0);
            } catch (HttpClientErrorException.TooManyRequests e) {
                logger.warn("Yahoo Finance returned 429 Too Many Requests for symbol {} on attempt {}", normalizedSymbol, attempt);
                if (attempt >= MAX_RETRIES) {
                    throw new YahooFinanceException("Too many requests to Yahoo Finance for symbol: " + normalizedSymbol, e);
                }
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new YahooFinanceException("Interrupted while backing off for Yahoo Finance", ie);
                }
                backoff *= 2;
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == 404) {
                    throw new IllegalArgumentException("Invalid ticker symbol: " + normalizedSymbol, e);
                }
                throw new YahooFinanceException("Failed to fetch Yahoo Finance data for symbol: " + normalizedSymbol + ", status: " + e.getStatusCode(), e);
            } catch (RestClientException e) {
                throw new YahooFinanceException("Failed to fetch Yahoo Finance data for symbol: " + normalizedSymbol, e);
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new YahooFinanceException("Failed to parse Yahoo Finance response for symbol: " + normalizedSymbol, e);
            }
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        return headers;
    }

    public static class HistoricalPricePoint {
        private final LocalDate tradingDate;
        private final BigDecimal closePrice;

        public HistoricalPricePoint(LocalDate tradingDate, BigDecimal closePrice) {
            this.tradingDate = tradingDate;
            this.closePrice = closePrice;
        }

        public LocalDate getTradingDate() {
            return tradingDate;
        }

        public BigDecimal getClosePrice() {
            return closePrice;
        }
    }
}


