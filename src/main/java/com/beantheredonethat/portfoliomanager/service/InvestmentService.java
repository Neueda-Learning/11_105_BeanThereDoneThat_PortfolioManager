package com.beantheredonethat.portfoliomanager.service;

import com.beantheredonethat.portfoliomanager.dto.CreateInvestmentRequest;
import com.beantheredonethat.portfoliomanager.dto.InvestmentResponse;
import com.beantheredonethat.portfoliomanager.dto.SymbolResolutionResult;
import com.beantheredonethat.portfoliomanager.dto.UpdateInvestmentRequest;
import com.beantheredonethat.portfoliomanager.entity.Investment;
import com.beantheredonethat.portfoliomanager.exception.InvestmentNotFoundException;
import com.beantheredonethat.portfoliomanager.exception.PortfolioNotFoundException;
import com.beantheredonethat.portfoliomanager.exception.YahooFinanceException;
import com.beantheredonethat.portfoliomanager.marketdata.AssetType;
import com.beantheredonethat.portfoliomanager.marketdata.MarketDataFactory;
import com.beantheredonethat.portfoliomanager.marketdata.MarketDataRequest;
import com.beantheredonethat.portfoliomanager.marketdata.MarketDataResponse;
import com.beantheredonethat.portfoliomanager.repository.InvestmentRepository;
import com.beantheredonethat.portfoliomanager.repository.PortfolioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class InvestmentService {

    private static final Logger logger =
            LoggerFactory.getLogger(InvestmentService.class);


    private final InvestmentRepository investmentRepository;
    private final PortfolioRepository portfolioRepository;
    private final MarketDataFactory marketDataFactory;
    private final SymbolResolverService symbolResolverService;


    public InvestmentService(
            InvestmentRepository investmentRepository,
            PortfolioRepository portfolioRepository,
            MarketDataFactory marketDataFactory,
            SymbolResolverService symbolResolverService) {

        this.investmentRepository = investmentRepository;
        this.portfolioRepository = portfolioRepository;
        this.marketDataFactory = marketDataFactory;
        this.symbolResolverService = symbolResolverService;
    }


    public InvestmentResponse createInvestment(
            CreateInvestmentRequest request) {


        portfolioRepository.findById(request.getPortfolioId())
                .orElseThrow(() ->
                        new PortfolioNotFoundException(
                                "Portfolio not found with ID: "
                                        + request.getPortfolioId()));


        BigDecimal investedAmount =
                request.getQuantity()
                        .multiply(request.getPurchasePrice())
                        .setScale(2, RoundingMode.HALF_UP);



        String originalSymbol =
                request.getSymbol() == null
                        ? null
                        : request.getSymbol()
                        .trim()
                        .toUpperCase();



        SymbolResolutionResult resolution =
                symbolResolverService.resolveSymbol(
                        originalSymbol,
                        request.getAssetType());



        String resolvedSymbol =
                resolution.getResolvedSymbol() != null
                        ? resolution.getResolvedSymbol()
                        : originalSymbol;


        String resolvedExchange =
                resolution.getExchange();


        String resolvedCurrency =
                resolution.getCurrency();


        String resolvedName =
                resolution.getAssetName();



        BigDecimal currentPrice =
                request.getCurrentPrice();

        BigDecimal currentValue = null;

        BigDecimal profitLoss = null;



        try {

            AssetType assetType =
                    parseAssetType(
                            request.getAssetType());


            MarketDataRequest marketRequest =
                    new MarketDataRequest();


            marketRequest.setAssetType(
                    assetType);


            marketRequest.setSymbol(
                    resolvedSymbol);


            marketRequest.setSchemeCode(
                    request.getSchemeCode());



            MarketDataResponse response =
                    marketDataFactory
                            .getService(assetType)
                            .getCurrentPrice(marketRequest);



            if(response != null &&
                    response.getPrice() != null) {


                currentPrice =
                        response.getPrice()
                                .setScale(
                                        2,
                                        RoundingMode.HALF_UP);



                currentValue =
                        currentPrice
                                .multiply(
                                        request.getQuantity())
                                .setScale(
                                        2,
                                        RoundingMode.HALF_UP);



                profitLoss =
                        currentValue
                                .subtract(
                                        investedAmount)
                                .setScale(
                                        2,
                                        RoundingMode.HALF_UP);
            }


        } catch(Exception e) {

            logger.warn(
                    "Unable to fetch market price: {}",
                    e.getMessage());
        }



        Investment investment =
                new Investment();



        investment.setPortfolioId(
                request.getPortfolioId());


        investment.setSymbol(
                resolvedSymbol);


        investment.setSchemeCode(
                request.getSchemeCode());


        investment.setCompanyName(
                resolvedName != null &&
                        !resolvedName.isBlank()
                        ? resolvedName
                        : request.getCompanyName());


        investment.setExchange(
                resolvedExchange);


        investment.setCurrency(
                resolvedCurrency);


        investment.setAssetType(
                request.getAssetType());


        investment.setCustomAssetType(
                request.getCustomAssetType());


        investment.setQuantity(
                request.getQuantity());


        investment.setInvestedAmount(
                investedAmount);


        investment.setPurchasePrice(
                request.getPurchasePrice());


        investment.setCurrentPrice(
                currentPrice);


        investment.setCurrentValue(
                currentValue);


        investment.setProfitLoss(
                profitLoss);


        investment.setPurchaseDate(
                request.getPurchaseDate());



        Investment saved =
                investmentRepository.insertInvestment(
                        investment);


        return mapToInvestmentResponse(saved);
    }



    public InvestmentResponse getInvestmentById(Integer id) {


        Investment investment =
                investmentRepository.findInvestmentById(id)
                        .orElseThrow(() ->
                                new InvestmentNotFoundException(
                                        "Investment not found with ID: "
                                                + id));


        refreshMarketValues(investment);


        return mapToInvestmentResponse(investment);
    }



    public List<InvestmentResponse> getAllInvestments() {

        List<Investment> investments =
                investmentRepository.findAllInvestments();


        for(Investment investment : investments) {

            try {

                refreshMarketValues(investment);

            } catch(Exception e) {

                logger.warn(
                        "Unable to refresh investment {} : {}",
                        investment.getInvestmentId(),
                        e.getMessage());
            }
        }


        return investments.stream()
                .map(this::mapToInvestmentResponse)
                .collect(Collectors.toList());
    }



    public List<InvestmentResponse> getInvestmentsByPortfolio(
            Integer portfolioId) {


        portfolioRepository.findById(portfolioId)
                .orElseThrow(() ->
                        new PortfolioNotFoundException(
                                "Portfolio not found with ID: "
                                        + portfolioId));


        List<Investment> investments =
                investmentRepository.findByPortfolioId(portfolioId);



        for(Investment investment : investments) {

            try {

                refreshMarketValues(investment);

            } catch(Exception e) {

                logger.warn(
                        "Unable to refresh investment {} : {}",
                        investment.getInvestmentId(),
                        e.getMessage());
            }
        }


        return investments.stream()
                .map(this::mapToInvestmentResponse)
                .collect(Collectors.toList());
    }

    public InvestmentResponse updateInvestment(
            Integer id,
            UpdateInvestmentRequest request) {


        Investment investment =
                investmentRepository.findInvestmentById(id)
                        .orElseThrow(() ->
                                new InvestmentNotFoundException(
                                        "Investment not found"));



        if(request.getQuantity() != null) {

            investment.setQuantity(
                    request.getQuantity());
        }



        if(request.getPurchasePrice() != null) {

            investment.setPurchasePrice(
                    request.getPurchasePrice());
        }



        if(request.getPurchaseDate() != null) {

            investment.setPurchaseDate(
                    request.getPurchaseDate());
        }



        if(request.getAssetType() != null) {

            investment.setAssetType(
                    request.getAssetType());
        }



        if(request.getCustomAssetType() != null) {

            investment.setCustomAssetType(
                    request.getCustomAssetType());
        }



        if(request.getSchemeCode() != null) {

            investment.setSchemeCode(
                    request.getSchemeCode());
        }



        investment.setInvestedAmount(
                investment.getQuantity()
                        .multiply(
                                investment.getPurchasePrice())
                        .setScale(
                                2,
                                RoundingMode.HALF_UP));



        refreshMarketValues(investment);



        investmentRepository.updateInvestment(
                investment);



        return mapToInvestmentResponse(investment);
    }





    public void deleteInvestment(Integer id) {


        investmentRepository.findInvestmentById(id)
                .orElseThrow(() ->
                        new InvestmentNotFoundException(
                                "Investment not found"));


        investmentRepository.deleteInvestment(id);
    }





    private void refreshMarketValues(
            Investment investment) {


        try {


            AssetType assetType =
                    parseAssetType(
                            investment.getAssetType());



            MarketDataRequest request =
                    new MarketDataRequest();



            request.setAssetType(
                    assetType);



            request.setSymbol(
                    investment.getSymbol());



            request.setSchemeCode(
                    investment.getSchemeCode());



            MarketDataResponse response =
                    marketDataFactory
                            .getService(assetType)
                            .getCurrentPrice(request);



            if(response != null &&
                    response.getPrice() != null) {


                BigDecimal currentPrice =
                        response.getPrice()
                                .setScale(
                                        2,
                                        RoundingMode.HALF_UP);



                BigDecimal currentValue =
                        currentPrice
                                .multiply(
                                        investment.getQuantity())
                                .setScale(
                                        2,
                                        RoundingMode.HALF_UP);



                BigDecimal profitLoss =
                        currentValue
                                .subtract(
                                        investment.getInvestedAmount())
                                .setScale(
                                        2,
                                        RoundingMode.HALF_UP);



                investment.setCurrentPrice(
                        currentPrice);



                investment.setCurrentValue(
                        currentValue);



                investment.setProfitLoss(
                        profitLoss);



                investmentRepository.updateMarketValues(
                        investment.getInvestmentId(),
                        currentPrice,
                        currentValue,
                        profitLoss);
            }



        } catch(YahooFinanceException yfe) {

            logger.warn(
                    "Unable to refresh market values for symbol {} : {}",
                    investment.getSymbol(),
                    yfe.getMessage());


        } catch(IllegalArgumentException iae) {

            logger.warn(
                    "Invalid symbol {} : {}",
                    investment.getSymbol(),
                    iae.getMessage());


        } catch(Exception e) {

            logger.warn(
                    "Unable to refresh investment {} : {}",
                    investment.getInvestmentId(),
                    e.getMessage());
        }
    }





    private InvestmentResponse mapToInvestmentResponse(
            Investment investment) {

        return new InvestmentResponse(investment);
    }





    private AssetType parseAssetType(
            String assetType) {


        if(assetType == null) {

            return AssetType.OTHER;
        }


        String normalized =
                assetType
                        .trim()
                        .toUpperCase();



        if(normalized.equals("CRYPTOCURRENCY")) {

            normalized = "CRYPTO";

        }



        try {

            return AssetType.valueOf(normalized);


        } catch(Exception e) {

            return AssetType.OTHER;

        }
    }

}