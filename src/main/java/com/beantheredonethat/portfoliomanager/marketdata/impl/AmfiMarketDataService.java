package com.beantheredonethat.portfoliomanager.marketdata.impl;

import com.beantheredonethat.portfoliomanager.exception.MarketDataException;
import com.beantheredonethat.portfoliomanager.marketdata.AssetType;
import com.beantheredonethat.portfoliomanager.marketdata.MarketDataRequest;
import com.beantheredonethat.portfoliomanager.marketdata.MarketDataResponse;
import com.beantheredonethat.portfoliomanager.marketdata.MarketDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple AMFI NAVAll.txt based implementation.
 * This implementation performs a best-effort text search for the scheme code and extracts the NAV value.
 */
@Service
public class AmfiMarketDataService implements MarketDataService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String AMFI_URL =
            "https://portal.amfiindia.com/spages/NAVAll.txt";

    private static final Logger logger = LoggerFactory.getLogger(AmfiMarketDataService.class);


    @Override
    public Set<AssetType> supportedAssetTypes() {
        return Set.of(AssetType.MUTUAL_FUND);
    }


    @Override
    public MarketDataResponse getCurrentPrice(MarketDataRequest request) {

        if (request == null) {
            throw new MarketDataException(
                    "MarketDataRequest cannot be null"
            );
        }


        String schemeCode = request.getSchemeCode();


        if (schemeCode == null || schemeCode.isBlank()) {
            throw new MarketDataException(
                    "schemeCode is required for mutual funds (AMFI)"
            );
        }


        try {

            String body =
                    restTemplate.getForObject(
                            AMFI_URL,
                            String.class
                    );


            if (body == null) {
                throw new MarketDataException(
                        "Empty response from AMFI"
                );
            }


            String[] lines = body.split("\r?\n");

            logger.debug("AMFI file contains {} lines", lines.length);

            for (String line : lines) {
                if (line == null || line.isBlank()) continue;

                String[] parts = line.split(";");

                if (parts.length > 4) {
                    String rawCode = parts[0].trim();
                    String rawCodeNorm = rawCode.replaceFirst("^0+", "");
                    String schemeNorm = schemeCode.replaceFirst("^0+", "");

                    if (rawCode.equalsIgnoreCase(schemeCode) || rawCodeNorm.equalsIgnoreCase(schemeNorm)) {
                        try {
                            String navToken = parts[4].trim();
                            logger.info("AMFI matched line for schemeCode={} -> navToken={}", schemeCode, navToken);
                            // remove any thousand separators
                            String cleaned = navToken.replaceAll(",", "");
                            BigDecimal price = new BigDecimal(cleaned);

                            MarketDataResponse resp = new MarketDataResponse();
                            resp.setPrice(price);
                            resp.setCurrency("INR");
                            resp.setTimestamp(Instant.now());
                            resp.setAssetType(AssetType.MUTUAL_FUND);
                            resp.setProviderId("AMFI");
                            return resp;
                        } catch (Exception ex) {
                            logger.warn("Failed to parse NAV '{}' for schemeCode={}", parts[4], schemeCode, ex);
                            throw new MarketDataException("Failed to parse NAV value for schemeCode=" + schemeCode, ex);
                        }
                    }
                }
            }


            throw new MarketDataException(
                    "Scheme code not found in AMFI data: "
                            + schemeCode
            );


        } catch (MarketDataException e) {

            throw e;


        } catch (Exception e) {

            throw new MarketDataException(
                    "Failed to fetch AMFI NAV data",
                    e
            );
        }
    }
}