package com.beantheredonethat.portfoliomanager.marketdata.impl;

import com.beantheredonethat.portfoliomanager.exception.MarketDataException;
import com.beantheredonethat.portfoliomanager.marketdata.AssetType;
import com.beantheredonethat.portfoliomanager.marketdata.MarketDataRequest;
import com.beantheredonethat.portfoliomanager.marketdata.MarketDataResponse;
import com.beantheredonethat.portfoliomanager.marketdata.MarketDataService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Set;


@Service
public class GoldMarketDataService implements MarketDataService {


    private final RestTemplate restTemplate =
            new RestTemplate();


    private final ObjectMapper objectMapper =
            new ObjectMapper();



    @Value("${gold.api.baseUrl}")
    private String baseUrl;


    @Value("${gold.api.key}")
    private String apiKey;



    @Override
    public Set<AssetType> supportedAssetTypes() {

        return Set.of(AssetType.GOLD);

    }



    @Override
    public MarketDataResponse getCurrentPrice(
            MarketDataRequest request) {


        if (request == null) {

            throw new MarketDataException(
                    "MarketDataRequest cannot be null"
            );

        }


        System.out.println(
                "Gold symbol received: "
                        + request.getSymbol()
        );


        try {


            HttpHeaders headers =
                    new HttpHeaders();


            headers.set(
                    "x-access-token",
                    apiKey
            );


            HttpEntity<String> entity =
                    new HttpEntity<>(
                            headers
                    );


            System.out.println(
                    "Calling Gold API URL: "
                            + baseUrl
            );



            ResponseEntity<String> response =
                    restTemplate.exchange(
                            baseUrl,
                            HttpMethod.GET,
                            entity,
                            String.class
                    );



            String body =
                    response.getBody();



            System.out.println(
                    "Gold API response: "
                            + body
            );



            if (body == null) {

                throw new MarketDataException(
                        "Empty response from Gold API"
                );

            }



            JsonNode root =
                    objectMapper.readTree(body);



            /*
              GoldAPI returns price as XAU price.
              XAU = 1 troy ounce of gold.

              Convert ounce price to gram price
              because our quantity is stored in grams.
            */

            BigDecimal gramPrice =
                    root.path("price_gram_24k")
                            .decimalValue();



            System.out.println(
                    "Gold price per gram: "
                            + gramPrice
            );



            MarketDataResponse responseData =
                    new MarketDataResponse();


            responseData.setPrice(
                    gramPrice
            );


            responseData.setCurrency(
                    "INR"
            );


            responseData.setTimestamp(
                    Instant.now()
            );


            responseData.setAssetType(
                    AssetType.GOLD
            );


            responseData.setProviderId(
                    "GOLDAPI"
            );


            return responseData;



        }
        catch (MarketDataException e) {

            throw e;

        }
        catch (Exception e) {

            throw new MarketDataException(
                    "Failed to fetch gold price from Gold API",
                    e
            );

        }

    }

}