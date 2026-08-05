package com.beantheredonethat.portfoliomanager.service;

import com.beantheredonethat.portfoliomanager.dto.CreateInvestmentRequest;
import com.beantheredonethat.portfoliomanager.dto.ExchangeRateResponse;
import com.beantheredonethat.portfoliomanager.dto.ImportFailureResponse;
import com.beantheredonethat.portfoliomanager.dto.ImportSummaryResponse;
import com.beantheredonethat.portfoliomanager.dto.InvestmentResponse;
import com.beantheredonethat.portfoliomanager.dto.SymbolResolutionResult;
import com.beantheredonethat.portfoliomanager.dto.UpdateInvestmentRequest;
import com.beantheredonethat.portfoliomanager.entity.Investment;
import com.beantheredonethat.portfoliomanager.entity.Portfolio;
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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class InvestmentService {

        private static final List<String> INVESTMENT_EXPORT_HEADERS = List.of(
                        "investment_id",
                        "portfolio_id",
                        "portfolio_name",
                        "symbol",
                        "scheme_code",
                        "company_name",
                        "exchange",
                        "currency",
                        "asset_type",
                        "custom_asset_type",
                        "quantity",
                        "invested_amount",
                        "purchase_price",
                        "current_price",
                        "current_value",
                        "profit_loss",
                        "purchase_date");

    private static final Logger logger =
            LoggerFactory.getLogger(InvestmentService.class);


    private final InvestmentRepository investmentRepository;
    private final PortfolioRepository portfolioRepository;
    private final MarketDataFactory marketDataFactory;
        private final CurrencyConversionService currencyConversionService;
    private final SymbolResolverService symbolResolverService;
        private final TabularDataService tabularDataService;


    public InvestmentService(
            InvestmentRepository investmentRepository,
            PortfolioRepository portfolioRepository,
            MarketDataFactory marketDataFactory,
                        CurrencyConversionService currencyConversionService,
                        SymbolResolverService symbolResolverService,
                        TabularDataService tabularDataService) {

        this.investmentRepository = investmentRepository;
        this.portfolioRepository = portfolioRepository;
        this.marketDataFactory = marketDataFactory;
                this.currencyConversionService = currencyConversionService;
        this.symbolResolverService = symbolResolverService;
                this.tabularDataService = tabularDataService;
    }

        public String exportInvestmentsCsv(Integer customerId) {
                Map<Integer, String> portfolioNames = portfolioRepository.findByCustomerId(customerId)
                                .stream()
                                .collect(Collectors.toMap(
                                                Portfolio::getPortfolioId,
                                                portfolio -> portfolio.getPortfolioName() == null ? "" : portfolio.getPortfolioName(),
                                                (left, right) -> left,
                                                LinkedHashMap::new));

                List<List<String>> rows = investmentRepository.findByCustomerId(customerId)
                                .stream()
                                .map(investment -> List.of(
                                                stringValue(investment.getInvestmentId()),
                                                stringValue(investment.getPortfolioId()),
                                                portfolioNames.getOrDefault(investment.getPortfolioId(), ""),
                                                stringValue(investment.getSymbol()),
                                                stringValue(investment.getSchemeCode()),
                                                stringValue(investment.getCompanyName()),
                                                stringValue(investment.getExchange()),
                                                stringValue(investment.getCurrency()),
                                                stringValue(investment.getAssetType()),
                                                stringValue(investment.getCustomAssetType()),
                                                stringValue(investment.getQuantity()),
                                                stringValue(investment.getInvestedAmount()),
                                                stringValue(investment.getPurchasePrice()),
                                                stringValue(investment.getCurrentPrice()),
                                                stringValue(investment.getCurrentValue()),
                                                stringValue(investment.getProfitLoss()),
                                                stringValue(investment.getPurchaseDate())))
                                .collect(Collectors.toList());

                return tabularDataService.writeCsv(INVESTMENT_EXPORT_HEADERS, rows);
        }

        public String exportInvestmentsTemplateCsv() {
                return tabularDataService.writeTemplate(INVESTMENT_EXPORT_HEADERS);
        }

        public ImportSummaryResponse importInvestments(Integer customerId, MultipartFile file) {
                TabularDataService.ParsedTabularData tabularData = tabularDataService.read(file);
                tabularDataService.validateHeaders(tabularData.getHeaders(), INVESTMENT_EXPORT_HEADERS);

                List<Portfolio> portfolios = portfolioRepository.findByCustomerId(customerId);
                Map<Integer, Portfolio> portfoliosById = portfolios.stream()
                                .collect(Collectors.toMap(Portfolio::getPortfolioId, portfolio -> portfolio));
                Map<String, List<Portfolio>> portfoliosByName = portfolios.stream()
                                .collect(Collectors.groupingBy(
                                                portfolio -> normalizeLookupKey(portfolio.getPortfolioName()),
                                                LinkedHashMap::new,
                                                Collectors.toList()));

                int successfulCount = 0;
                List<ImportFailureResponse> failures = new ArrayList<>();

                for (TabularDataService.TabularRow row : tabularData.getRows()) {
                        try {
                                CreateInvestmentRequest request = mapImportedInvestment(row, portfoliosById, portfoliosByName);
                                createInvestment(customerId, request);
                                successfulCount++;
                        } catch (Exception ex) {
                                failures.add(new ImportFailureResponse(row.getRowNumber(), rootMessage(ex)));
                        }
                }

                return new ImportSummaryResponse(successfulCount, failures.size(), failures);
        }


    public InvestmentResponse createInvestment(
            Integer customerId,
            CreateInvestmentRequest request) {


        portfolioRepository.findByIdAndCustomerId(request.getPortfolioId(), customerId)
                .orElseThrow(() ->
                        new PortfolioNotFoundException(
                                "Portfolio not found with ID: "
                                        + request.getPortfolioId()));


        BigDecimal investedAmount =
                request.getQuantity()
                        .multiply(request.getPurchasePrice())
                        .setScale(2, RoundingMode.HALF_UP);


        String storedCurrency =
                normalizeCurrency(request.getCurrency());


        NormalizedIdentifier normalizedIdentifier =
                normalizeIdentifier(
                        request.getAssetType(),
                        request.getSymbol(),
                        request.getSchemeCode());



        String originalSymbol =
                normalizedIdentifier.symbol() == null
                        ? null
                        : normalizedIdentifier.symbol()
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
                    normalizedIdentifier.schemeCode());


            marketRequest.setCurrency(
                    storedCurrency);



            MarketDataResponse response =
                    marketDataFactory
                            .getService(assetType)
                            .getCurrentPrice(marketRequest);



            if(response != null &&
                    response.getPrice() != null) {


                BigDecimal priceInStoredCurrency =
                        convertToStoredCurrency(
                                response.getPrice(),
                                response.getCurrency(),
                                storedCurrency);


                currentPrice =
                        priceInStoredCurrency
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
                normalizedIdentifier.schemeCode());


        investment.setCompanyName(
                resolvedName != null &&
                        !resolvedName.isBlank()
                        ? resolvedName
                        : request.getCompanyName());


        investment.setExchange(
                resolvedExchange);


        investment.setCurrency(
                storedCurrency != null
                        ? storedCurrency
                        : resolvedCurrency);


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



        public InvestmentResponse getInvestmentById(Integer id, Integer customerId) {


        Investment investment =
                investmentRepository.findInvestmentByIdAndCustomerId(id, customerId)
                        .orElseThrow(() ->
                                new InvestmentNotFoundException(
                                        "Investment not found with ID: "
                                                + id));


        refreshMarketValues(investment);


        return mapToInvestmentResponse(investment);
    }



        public List<InvestmentResponse> getAllInvestments(Integer customerId) {

        List<Investment> investments =
                                investmentRepository.findByCustomerId(customerId);


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
            Integer portfolioId,
            Integer customerId) {


        portfolioRepository.findByIdAndCustomerId(portfolioId, customerId)
                .orElseThrow(() ->
                        new PortfolioNotFoundException(
                                "Portfolio not found with ID: "
                                        + portfolioId));


        List<Investment> investments =
                investmentRepository.findByPortfolioIdAndCustomerId(portfolioId, customerId);



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
            Integer customerId,
            UpdateInvestmentRequest request) {


        Investment investment =
                investmentRepository.findInvestmentByIdAndCustomerId(id, customerId)
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





        public void deleteInvestment(Integer id, Integer customerId) {


        investmentRepository.findInvestmentByIdAndCustomerId(id, customerId)
                .orElseThrow(() ->
                        new InvestmentNotFoundException(
                                "Investment not found"));


        investmentRepository.deleteInvestment(id);
    }



        public ExchangeRateResponse getExchangeRate(
                        String fromCurrency,
                        String toCurrency) {

                String normalizedFrom = normalizeCurrency(fromCurrency);
                String normalizedTo = normalizeCurrency(toCurrency);

                BigDecimal rate = currencyConversionService.getExchangeRate(
                                normalizedFrom,
                                normalizedTo);

                return new ExchangeRateResponse(
                                normalizedFrom,
                                normalizedTo,
                                rate);
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


            request.setCurrency(
                    investment.getCurrency());



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


                BigDecimal priceInStoredCurrency =
                        convertToStoredCurrency(
                                response.getPrice(),
                                response.getCurrency(),
                                investment.getCurrency());


                BigDecimal currentPrice =
                        priceInStoredCurrency
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

        private CreateInvestmentRequest mapImportedInvestment(
                        TabularDataService.TabularRow row,
                        Map<Integer, Portfolio> portfoliosById,
                        Map<String, List<Portfolio>> portfoliosByName) {

                CreateInvestmentRequest request = new CreateInvestmentRequest();
                request.setPortfolioId(resolvePortfolioId(row, portfoliosById, portfoliosByName));

                String assetType = requiredText(row, "asset_type");
                request.setAssetType(assetType);

                String normalizedType = normalizeAssetTypeKey(assetType);
                String symbol = optionalText(row, "symbol");
                String schemeCode = optionalText(row, "scheme_code");

                if ("MUTUAL FUND".equals(normalizedType) || "MUTUAL_FUND".equals(normalizedType)) {
                        if (schemeCode == null) {
                                throw new IllegalArgumentException("scheme_code is required for mutual fund investments.");
                        }
                        request.setSchemeCode(schemeCode);
                } else if (!"GOLD".equals(normalizedType)) {
                        if (symbol == null) {
                                throw new IllegalArgumentException("symbol is required for this asset type.");
                        }
                        request.setSymbol(symbol.toUpperCase(Locale.ROOT));
                }

                request.setCompanyName(requiredText(row, "company_name"));
                request.setCurrency(requiredText(row, "currency").toUpperCase(Locale.ROOT));
                request.setQuantity(positiveDecimal(row, "quantity"));
                request.setPurchasePrice(positiveDecimal(row, "purchase_price"));
                request.setPurchaseDate(requiredDate(row, "purchase_date"));
                request.setCurrentPrice(optionalPositiveDecimal(row, "current_price"));

                String customAssetType = optionalText(row, "custom_asset_type");
                if ("CUSTOM".equals(normalizedType) && customAssetType == null) {
                        throw new IllegalArgumentException("custom_asset_type is required when asset_type is Custom.");
                }
                request.setCustomAssetType(customAssetType);

                return request;
        }

        private Integer resolvePortfolioId(
                        TabularDataService.TabularRow row,
                        Map<Integer, Portfolio> portfoliosById,
                        Map<String, List<Portfolio>> portfoliosByName) {

                String portfolioIdText = optionalText(row, "portfolio_id");
                if (portfolioIdText != null) {
                        Integer portfolioId = positiveInteger(portfolioIdText, "portfolio_id");
                        if (portfoliosById.containsKey(portfolioId)) {
                                return portfolioId;
                        }
                        throw new IllegalArgumentException("portfolio_id does not belong to the current user.");
                }

                String portfolioName = requiredText(row, "portfolio_name");
                List<Portfolio> matches = portfoliosByName.getOrDefault(normalizeLookupKey(portfolioName), List.of());
                if (matches.isEmpty()) {
                        throw new IllegalArgumentException("portfolio_name was not found for the current user.");
                }
                if (matches.size() > 1) {
                        throw new IllegalArgumentException("portfolio_name is ambiguous. Use portfolio_id instead.");
                }
                return matches.get(0).getPortfolioId();
        }

        private String requiredText(TabularDataService.TabularRow row, String header) {
                String value = optionalText(row, header);
                if (value == null) {
                        throw new IllegalArgumentException(header + " is required.");
                }
                return value;
        }

        private String optionalText(TabularDataService.TabularRow row, String header) {
                String value = row.get(header);
                if (value == null) {
                        return null;
                }
                String trimmed = value.trim();
                return trimmed.isEmpty() ? null : trimmed;
        }

        private BigDecimal positiveDecimal(TabularDataService.TabularRow row, String header) {
                BigDecimal value = decimalValue(requiredText(row, header), header);
                if (value.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException(header + " must be greater than 0.");
                }
                return value;
        }

        private BigDecimal optionalPositiveDecimal(TabularDataService.TabularRow row, String header) {
                String value = optionalText(row, header);
                if (value == null) {
                        return null;
                }
                BigDecimal parsed = decimalValue(value, header);
                if (parsed.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException(header + " must be greater than 0 when provided.");
                }
                return parsed;
        }

        private BigDecimal decimalValue(String raw, String header) {
                try {
                        return new BigDecimal(raw.trim());
                } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException(header + " must be a valid number.");
                }
        }

        private LocalDate requiredDate(TabularDataService.TabularRow row, String header) {
                try {
                        return LocalDate.parse(requiredText(row, header));
                } catch (DateTimeParseException ex) {
                        throw new IllegalArgumentException(header + " must be a valid date in yyyy-MM-dd format.");
                }
        }

        private Integer positiveInteger(String raw, String header) {
                try {
                        int value = Integer.parseInt(raw.trim());
                        if (value <= 0) {
                                throw new IllegalArgumentException(header + " must be greater than 0.");
                        }
                        return value;
                } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException(header + " must be a valid integer.");
                }
        }

        private String normalizeAssetTypeKey(String assetType) {
                return assetType == null ? "" : assetType.trim().toUpperCase(Locale.ROOT);
        }

        private String normalizeLookupKey(String value) {
                return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        }

        private String stringValue(Object value) {
                return value == null ? "" : String.valueOf(value);
        }

        private String rootMessage(Throwable throwable) {
                Throwable current = throwable;
                while (current.getCause() != null && current.getCause() != current) {
                        current = current.getCause();
                }

                String message = current.getMessage();
                return message == null || message.isBlank() ? "Unable to import this row." : message;
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



    private String normalizeCurrency(
            String currency) {

        if(currency == null || currency.isBlank()) {

            return null;
        }


        return currency.trim().toUpperCase();
    }



    private BigDecimal convertToStoredCurrency(
            BigDecimal amount,
            String sourceCurrency,
            String targetCurrency) {

        String normalizedSource =
                normalizeCurrency(sourceCurrency);


        String normalizedTarget =
                normalizeCurrency(targetCurrency);


        if(amount == null
                || normalizedSource == null
                || normalizedTarget == null
                || normalizedSource.equals(normalizedTarget)) {

            return amount;
        }


        return currencyConversionService.convert(
                amount,
                normalizedSource,
                normalizedTarget);
    }



    private NormalizedIdentifier normalizeIdentifier(
            String assetType,
            String symbol,
            String schemeCode) {

        String normalizedType =
                assetType == null
                        ? ""
                        : assetType.trim().toUpperCase();


        String normalizedSymbol =
                symbol == null || symbol.isBlank()
                        ? null
                        : symbol.trim().toUpperCase();


        String normalizedSchemeCode =
                schemeCode == null || schemeCode.isBlank()
                        ? null
                        : schemeCode.trim();


        if("MUTUAL FUND".equals(normalizedType)
                || "MUTUAL_FUND".equals(normalizedType)) {

            return new NormalizedIdentifier(
                    null,
                    normalizedSchemeCode);
        }


        if("GOLD".equals(normalizedType)) {

            return new NormalizedIdentifier(
                    null,
                    null);
        }


        return new NormalizedIdentifier(
                normalizedSymbol,
                null);
    }



    private record NormalizedIdentifier(
            String symbol,
            String schemeCode) {
    }

}