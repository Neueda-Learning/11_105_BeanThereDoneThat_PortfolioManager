package com.beantheredonethat.portfoliomanager.marketdata.impl;

import com.beantheredonethat.portfoliomanager.exception.MarketDataException;
import com.beantheredonethat.portfoliomanager.marketdata.AssetType;
import com.beantheredonethat.portfoliomanager.marketdata.MarketDataRequest;
import com.beantheredonethat.portfoliomanager.marketdata.MarketDataResponse;
import com.beantheredonethat.portfoliomanager.marketdata.MarketDataService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;


@Service
public class CryptoMarketDataService implements MarketDataService {


    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public Set<AssetType> supportedAssetTypes() {

        return Set.of(AssetType.CRYPTO);

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
                "Crypto symbol received: "
                        + request.getSymbol()
        );


        String cryptoId =
                getCoinGeckoId(request.getSymbol());


        System.out.println(
                "CoinGecko ID found: "
                        + cryptoId
        );


        String currency =
                request.getCurrency() == null
                        ? "inr"
                        : request.getCurrency().toLowerCase();



        String url = String.format(
                "https://api.coingecko.com/api/v3/simple/price?ids=%s&vs_currencies=%s",
                cryptoId,
                currency
        );


        System.out.println(
                "Calling CoinGecko URL: "
                        + url
        );


        try {


            ResponseEntity<String> response =
                    restTemplate.getForEntity(
                            url,
                            String.class
                    );


            String body = response.getBody();


            System.out.println(
                    "CoinGecko price response: "
                            + body
            );


            if (body == null) {

                throw new MarketDataException(
                        "Empty response from CoinGecko"
                );

            }


            JsonNode root =
                    objectMapper.readTree(body);



            JsonNode priceNode =
                    root.path(cryptoId)
                            .path(currency);



            System.out.println(
                    "Extracted price node: "
                            + priceNode
            );


            if (priceNode.isMissingNode()
                    || priceNode.isNull()) {


                throw new MarketDataException(
                        "Price not found for crypto: "
                                + cryptoId
                );

            }



            BigDecimal price =
                    priceNode.decimalValue();



            System.out.println(
                    "Final crypto price: "
                            + price
            );



            MarketDataResponse responseData =
                    new MarketDataResponse();


            responseData.setPrice(price);

            responseData.setCurrency(
                    currency.toUpperCase()
            );

            responseData.setTimestamp(
                    Instant.now()
            );

            responseData.setAssetType(
                    AssetType.CRYPTO
            );

            responseData.setProviderId(
                    "COINGECKO"
            );


            return responseData;



        } catch (MarketDataException e) {

            throw e;


        } catch (Exception e) {

            throw new MarketDataException(
                    "Failed to fetch crypto price from CoinGecko",
                    e
            );

        }

    }




    private String getCoinGeckoId(String symbol) {


        if (symbol == null || symbol.isBlank()) {

            throw new MarketDataException(
                    "Crypto symbol is required"
            );

        }


        symbol = symbol.toLowerCase();



        System.out.println(
                "Searching CoinGecko ID for symbol: "
                        + symbol
        );



        /*
          Common crypto symbol mapping.
          Prevents duplicate symbol issues in CoinGecko.
        */
        Map<String, String> cryptoMapping = Map.of(

                "btc", "bitcoin",

                "eth", "ethereum",

                "usdt", "tether",

                "bnb", "binancecoin",

                "sol", "solana",

                "xrp", "ripple",

                "ada", "cardano",

                "doge", "dogecoin"

        );



        if (cryptoMapping.containsKey(symbol)) {

            return cryptoMapping.get(symbol);

        }



        String url =
                "https://api.coingecko.com/api/v3/coins/list";



        try {


            ResponseEntity<String> response =
                    restTemplate.getForEntity(
                            url,
                            String.class
                    );



            JsonNode coins =
                    objectMapper.readTree(
                            response.getBody()
                    );



            for (JsonNode coin : coins) {


                String coinSymbol =
                        coin.path("symbol")
                                .asText();



                if (coinSymbol.equalsIgnoreCase(symbol)) {


                    System.out.println(
                            "Matched CoinGecko coin: "
                                    + coin
                    );


                    return coin.path("id")
                            .asText();

                }

            }



            throw new MarketDataException(
                    "Crypto not found: " + symbol
            );



        } catch (MarketDataException e) {


            throw e;


        } catch (Exception e) {


            throw new MarketDataException(
                    "Failed to fetch crypto list from CoinGecko",
                    e
            );

        }

    }

}