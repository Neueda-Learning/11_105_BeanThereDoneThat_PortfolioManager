package com.beantheredonethat.portfoliomanager.service;

import com.beantheredonethat.portfoliomanager.dto.SymbolResolutionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

@Service
public class SymbolResolverService {

    private static final Logger logger = LoggerFactory.getLogger(SymbolResolverService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SymbolResolverService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public SymbolResolutionResult resolveSymbol(String symbol, String assetType) {
        String normalizedSymbol = normalizeSymbol(symbol);
        if (normalizedSymbol == null) {
            return unresolved(symbol);
        }

        String normalizedAssetType = normalizeAssetType(assetType);
        if (normalizedAssetType == null) {
            logger.warn("Unsupported asset type '{}' for symbol '{}'. Falling back to manual pricing flow.", assetType, normalizedSymbol);
            return unresolved(normalizedSymbol);
        }

        try {
            String url = "https://query1.finance.yahoo.com/v1/finance/search?q="
                    + URLEncoder.encode(normalizedSymbol, StandardCharsets.UTF_8)
                    + "&quotesCount=20&newsCount=0";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    String.class
            );

            String body = response.getBody();
            if (body == null || body.isBlank()) {
                return unresolved(normalizedSymbol);
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode quotes = root.path("quotes");
            if (!quotes.isArray() || quotes.isEmpty()) {
                return unresolved(normalizedSymbol);
            }

            JsonNode best = selectBestQuote(quotes, normalizedSymbol, normalizedAssetType);
            if (best == null) {
                return unresolved(normalizedSymbol);
            }

            String resolved = readText(best, "symbol");
            if (resolved == null || resolved.isBlank()) {
                return unresolved(normalizedSymbol);
            }

            String exchange = firstNonBlank(
                    readText(best, "exchDisp"),
                    readText(best, "exchange"),
                    readText(best, "fullExchangeName")
            );

            String currency = readText(best, "currency");
            String assetName = firstNonBlank(
                    readText(best, "longname"),
                    readText(best, "shortname"),
                    readText(best, "name")
            );

            return new SymbolResolutionResult(
                    resolved.toUpperCase(Locale.ROOT),
                    isBlank(exchange) ? null : exchange,
                    isBlank(currency) ? null : currency.toUpperCase(Locale.ROOT),
                    isBlank(assetName) ? null : assetName
            );
        } catch (RestClientException ex) {
            logger.warn("Yahoo search failed for symbol '{}': {}", normalizedSymbol, ex.getMessage());
            return unresolved(normalizedSymbol);
        } catch (Exception ex) {
            logger.warn("Symbol resolution failed for symbol '{}': {}", normalizedSymbol, ex.getMessage());
            return unresolved(normalizedSymbol);
        }
    }

    private JsonNode selectBestQuote(JsonNode quotes, String inputSymbol, String normalizedAssetType) {
        Set<String> expectedTypes = expectedQuoteTypes(normalizedAssetType);

        JsonNode best = null;
        int bestScore = Integer.MIN_VALUE;

        for (JsonNode quote : quotes) {
            String candidateSymbol = readText(quote, "symbol");
            if (isBlank(candidateSymbol)) {
                continue;
            }

            String quoteType = normalizeQuoteType(readText(quote, "quoteType"));
            int score = scoreCandidate(inputSymbol, candidateSymbol, expectedTypes, quoteType, normalizedAssetType);
            if (score > bestScore) {
                bestScore = score;
                best = quote;
            }
        }

        return bestScore >= 0 ? best : null;
    }

    private int scoreCandidate(String inputSymbol,
                               String candidateSymbol,
                               Set<String> expectedTypes,
                               String quoteType,
                               String normalizedAssetType) {
        int score = 0;

        String input = inputSymbol.toUpperCase(Locale.ROOT);
        String candidate = candidateSymbol.toUpperCase(Locale.ROOT);

        if (candidate.equals(input)) {
            score += 120;
        } else if (candidate.startsWith(input + ".") || candidate.startsWith(input + "-")) {
            score += 100;
        } else if (candidate.startsWith(input)) {
            score += 80;
        } else if (candidate.contains(input)) {
            score += 40;
        } else {
            score -= 20;
        }

        if (!expectedTypes.isEmpty()) {
            if (expectedTypes.contains(quoteType)) {
                score += 60;
            } else {
                score -= 25;
            }
        }

        if ("CRYPTO".equals(normalizedAssetType) && candidate.endsWith("-USD")) {
            score += 30;
        }

        if ("STOCK".equals(normalizedAssetType) && (candidate.contains(".") || !candidate.contains("-"))) {
            score += 15;
        }

        return score;
    }

    private Set<String> expectedQuoteTypes(String normalizedAssetType) {
        return switch (normalizedAssetType) {
            case "STOCK" -> Set.of("EQUITY");
            case "MUTUAL_FUND" -> Set.of("MUTUALFUND");
            case "ETF" -> Set.of("ETF");
            case "CRYPTO" -> Set.of("CRYPTOCURRENCY");
            case "BOND" -> Set.of("BOND");
            case "GOLD" -> Set.of("COMMODITY", "ETF", "FUTURE");
            case "CUSTOM" -> Set.of();
            default -> Set.of();
        };
    }

    private String normalizeAssetType(String assetType) {
        if (assetType == null) {
            return null;
        }

        String normalized = assetType.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        return switch (normalized) {
            case "STOCK", "EQUITY" -> "STOCK";
            case "MUTUAL_FUND", "MUTUALFUND" -> "MUTUAL_FUND";
            case "ETF" -> "ETF";
            case "CRYPTO", "CRYPTOCURRENCY" -> "CRYPTO";
            case "BOND" -> "BOND";
            case "GOLD" -> "GOLD";
            case "CUSTOM" -> "CUSTOM";
            default -> null;
        };
    }

    private String normalizeQuoteType(String quoteType) {
        if (quoteType == null) {
            return "";
        }
        return quoteType.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null) {
            return null;
        }

        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private String readText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null ? null : text.trim();
    }

    private SymbolResolutionResult unresolved(String originalSymbol) {
        return new SymbolResolutionResult(normalizeSymbol(originalSymbol), null, null, null);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        return headers;
    }
}