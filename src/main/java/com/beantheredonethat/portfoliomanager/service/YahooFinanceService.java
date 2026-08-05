package com.beantheredonethat.portfoliomanager.service;

import com.beantheredonethat.portfoliomanager.exception.MarketDataException;
import com.beantheredonethat.portfoliomanager.exception.YahooFinanceException;
import com.beantheredonethat.portfoliomanager.marketdata.AssetType;
import com.beantheredonethat.portfoliomanager.marketdata.MarketDataRequest;
import com.beantheredonethat.portfoliomanager.marketdata.MarketDataResponse;
import com.beantheredonethat.portfoliomanager.marketdata.MarketDataService;
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

@Service
public class YahooFinanceService implements MarketDataService {

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

    @Override
    public java.util.Set<AssetType> supportedAssetTypes() {
        return java.util.Set.of(AssetType.STOCK, AssetType.ETF);
    }

    @Override
    public MarketDataResponse getCurrentPrice(MarketDataRequest request) {
        if (request == null) {
            throw new MarketDataException("MarketDataRequest cannot be null");
        }
        String symbol = request.getSymbol();
        BigDecimal price = getCurrentPrice(symbol);
        MarketDataResponse resp = new MarketDataResponse();
        resp.setPrice(price);
        resp.setCurrency(request.getCurrency());
        resp.setTimestamp(java.time.Instant.now());
        resp.setAssetType(request.getAssetType());
        resp.setProviderId("YAHOO");
        return resp;
    }

    /**
     * Backward-compatible method kept for other callers.
     */
    public BigDecimal getCurrentPrice(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new YahooFinanceException("Invalid symbol");
        }

        String normalized = symbol.trim().toUpperCase();
        // Use the v8 chart endpoint which returns chart.result[0].meta.regularMarketPrice
            String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + normalized;

        // Prepare headers to mimic a real browser (may help avoid some rate limiting)
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        int attempt = 0;
        long backoff = INITIAL_BACKOFF_MS;
        while (true) {
            attempt++;
            try {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
                String resp = response.getBody();
                if (resp == null) {
                    throw new YahooFinanceException("Empty response from Yahoo Finance for symbol: " + normalized);
                }

                JsonNode root = objectMapper.readTree(resp);
                // v8/chart response: { "chart": { "result": [ { "meta": { "regularMarketPrice": ... } } ], "error": ... } }
                JsonNode chart = root.path("chart");
                if (chart.isMissingNode() || chart.isNull()) {
                    throw new YahooFinanceException("No chart data for symbol: " + normalized);
                }

                JsonNode results = chart.path("result");
                if (!results.isArray() || results.size() == 0) {
                    JsonNode errorNode = chart.path("error");
                    String err = errorNode.isMissingNode() || errorNode.isNull() ? "No result for symbol" : errorNode.toString();
                    throw new YahooFinanceException("No chart result for symbol: " + normalized + "; " + err);
                }

                JsonNode first = results.get(0);
                JsonNode meta = first.path("meta");
                if (meta.isMissingNode() || meta.isNull()) {
                    throw new YahooFinanceException("Missing meta section in chart result for symbol: " + normalized);
                }

                JsonNode priceNode = meta.path("regularMarketPrice");
                if (priceNode.isMissingNode() || priceNode.isNull()) {
                    throw new YahooFinanceException("Missing regularMarketPrice for symbol: " + normalized);
                }

                double price = priceNode.asDouble();
                return BigDecimal.valueOf(price);
            } catch (HttpClientErrorException.TooManyRequests e) {
                // 429 - back off and retry up to MAX_RETRIES
                logger.warn("Yahoo Finance returned 429 Too Many Requests for symbol {} on attempt {}", normalized, attempt);
                if (attempt >= MAX_RETRIES) {
                    throw new YahooFinanceException("Too many requests to Yahoo Finance for symbol: " + normalized, e);
                }
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new YahooFinanceException("Interrupted while backing off for Yahoo Finance", ie);
                }
                backoff *= 2;
                continue;
            } catch (HttpClientErrorException e) {
                // Non-rate-limit 4xx errors
                throw new YahooFinanceException("Failed to fetch price for symbol: " + normalized + ", status: " + e.getStatusCode(), e);
            } catch (RestClientException e) {
                // Other client-side errors (I/O, timeouts)
                throw new YahooFinanceException("Failed to fetch price for symbol: " + normalized, e);
            } catch (Exception e) {
                // JSON parse errors or other unexpected exceptions
                throw new YahooFinanceException("Failed to parse Yahoo Finance response for symbol: " + normalized, e);
            }
        }
    }
}


