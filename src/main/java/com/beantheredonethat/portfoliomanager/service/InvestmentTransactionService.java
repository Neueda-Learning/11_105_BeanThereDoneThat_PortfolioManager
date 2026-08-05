package com.beantheredonethat.portfoliomanager.service;

import com.beantheredonethat.portfoliomanager.dto.CreateTransactionRequest;
import com.beantheredonethat.portfoliomanager.dto.ImportFailureResponse;
import com.beantheredonethat.portfoliomanager.dto.ImportSummaryResponse;
import com.beantheredonethat.portfoliomanager.dto.TransactionResponse;
import com.beantheredonethat.portfoliomanager.entity.Investment;
import com.beantheredonethat.portfoliomanager.entity.InvestmentTransaction;
import com.beantheredonethat.portfoliomanager.entity.Portfolio;
import com.beantheredonethat.portfoliomanager.exception.InvestmentNotFoundException;
import com.beantheredonethat.portfoliomanager.exception.TransactionNotFoundException;
import com.beantheredonethat.portfoliomanager.repository.InvestmentRepository;
import com.beantheredonethat.portfoliomanager.repository.InvestmentTransactionRepository;
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
public class InvestmentTransactionService {

        private static final List<String> TRANSACTION_EXPORT_HEADERS = List.of(
                        "transaction_id",
                        "investment_id",
                        "portfolio_id",
                        "portfolio_name",
                        "symbol",
                        "scheme_code",
                        "company_name",
                        "asset_type",
                        "transaction_date",
                        "transaction_type",
                        "quantity",
                        "transaction_price",
                        "transaction_amount");

    private final InvestmentTransactionRepository transactionRepository;
    private final InvestmentRepository investmentRepository;
        private final com.beantheredonethat.portfoliomanager.repository.PortfolioRepository portfolioRepository;
        private final TabularDataService tabularDataService;

    public InvestmentTransactionService(
            InvestmentTransactionRepository transactionRepository,
                        InvestmentRepository investmentRepository,
                        com.beantheredonethat.portfoliomanager.repository.PortfolioRepository portfolioRepository,
                        TabularDataService tabularDataService) {

        this.transactionRepository = transactionRepository;
        this.investmentRepository = investmentRepository;
                this.portfolioRepository = portfolioRepository;
                this.tabularDataService = tabularDataService;
    }

        public String exportTransactionsCsv(Integer customerId) {
                Map<Integer, Investment> investmentsById = investmentRepository.findByCustomerId(customerId)
                                .stream()
                                .collect(Collectors.toMap(Investment::getInvestmentId, investment -> investment));

                Map<Integer, String> portfolioNames = portfolioRepository.findByCustomerId(customerId)
                                .stream()
                                .collect(Collectors.toMap(
                                                Portfolio::getPortfolioId,
                                                portfolio -> portfolio.getPortfolioName() == null ? "" : portfolio.getPortfolioName(),
                                                (left, right) -> left,
                                                LinkedHashMap::new));

                List<List<String>> rows = transactionRepository.findByCustomerId(customerId)
                                .stream()
                                .map(transaction -> {
                                        Investment investment = investmentsById.get(transaction.getInvestmentId());
                                        Integer portfolioId = investment == null ? null : investment.getPortfolioId();
                                        String portfolioName = portfolioId == null ? "" : portfolioNames.getOrDefault(portfolioId, "");
                                        String schemeCode = investment == null ? "" : stringValue(investment.getSchemeCode());

                                        return List.of(
                                                        stringValue(transaction.getTransactionId()),
                                                        stringValue(transaction.getInvestmentId()),
                                                        stringValue(portfolioId),
                                                        portfolioName,
                                                        stringValue(transaction.getSymbol()),
                                                        schemeCode,
                                                        stringValue(transaction.getCompanyName()),
                                                        stringValue(transaction.getAssetType()),
                                                        stringValue(transaction.getTransactionDate()),
                                                        stringValue(transaction.getTransactionType()),
                                                        stringValue(transaction.getQuantity()),
                                                        stringValue(transaction.getTransactionPrice()),
                                                        stringValue(transaction.getTransactionAmount()));
                                })
                                .collect(Collectors.toList());

                return tabularDataService.writeCsv(TRANSACTION_EXPORT_HEADERS, rows);
        }

        public String exportTransactionsTemplateCsv() {
                return tabularDataService.writeTemplate(TRANSACTION_EXPORT_HEADERS);
        }

        public ImportSummaryResponse importTransactions(Integer customerId, MultipartFile file) {
                TabularDataService.ParsedTabularData tabularData = tabularDataService.read(file);
                tabularDataService.validateHeaders(tabularData.getHeaders(), TRANSACTION_EXPORT_HEADERS);

                List<Investment> investments = investmentRepository.findByCustomerId(customerId);
                Map<Integer, Investment> investmentsById = investments.stream()
                                .collect(Collectors.toMap(Investment::getInvestmentId, investment -> investment));

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
                                Investment investment = resolveImportedInvestment(row, investments, investmentsById, portfoliosById, portfoliosByName);

                                CreateTransactionRequest request = new CreateTransactionRequest();
                                request.setInvestmentId(investment.getInvestmentId());
                                request.setTransactionType(requiredText(row, "transaction_type").toUpperCase(Locale.ROOT));
                                request.setQuantity(positiveDecimal(row, "quantity"));
                                request.setTransactionPrice(positiveDecimal(row, "transaction_price"));
                                request.setTransactionDate(requiredDate(row, "transaction_date"));

                                createTransaction(customerId, request);
                                successfulCount++;
                        } catch (Exception ex) {
                                failures.add(new ImportFailureResponse(row.getRowNumber(), rootMessage(ex)));
                        }
                }

                return new ImportSummaryResponse(successfulCount, failures.size(), failures);
        }

    public TransactionResponse createTransaction(
            Integer customerId,
            CreateTransactionRequest request) {

        Investment investment = investmentRepository
                .findInvestmentByIdAndCustomerId(request.getInvestmentId(), customerId)
                .orElseThrow(() ->
                        new InvestmentNotFoundException(
                                "Investment not found with ID: "
                                        + request.getInvestmentId()));

        BigDecimal transactionAmount =
                request.getQuantity()
                        .multiply(request.getTransactionPrice())
                        .setScale(2, RoundingMode.HALF_UP);

        String type = request.getTransactionType().toUpperCase();

        if ("BUY".equals(type)) {

            investment.setQuantity(
                    investment.getQuantity()
                            .add(request.getQuantity()));

        } else if ("SELL".equals(type)) {

            if (investment.getQuantity()
                    .compareTo(request.getQuantity()) < 0) {

                throw new RuntimeException(
                        "Insufficient quantity available");
            }

            investment.setQuantity(
                    investment.getQuantity()
                            .subtract(request.getQuantity()));

        } else {

            throw new RuntimeException(
                    "Transaction type must be BUY or SELL");
        }

        if (investment.getPurchasePrice() != null) {

            investment.setInvestedAmount(
                    investment.getQuantity()
                            .multiply(investment.getPurchasePrice())
                            .setScale(2, RoundingMode.HALF_UP));
        }

        investmentRepository.updateInvestment(investment);

        InvestmentTransaction transaction =
                new InvestmentTransaction();

        transaction.setInvestmentId(
                request.getInvestmentId());

        transaction.setTransactionDate(
                request.getTransactionDate());

        transaction.setTransactionType(
                type);

        transaction.setQuantity(
                request.getQuantity());

        transaction.setTransactionPrice(
                request.getTransactionPrice());

        transaction.setTransactionAmount(
                transactionAmount);

        InvestmentTransaction saved =
                transactionRepository.insertTransaction(
                        transaction);

        saved.setSymbol(investment.getSymbol());
        saved.setCompanyName(investment.getCompanyName());
        saved.setAssetType(investment.getAssetType());

        return new TransactionResponse(saved);
    }

    public TransactionResponse getTransactionById(
            Integer id,
            Integer customerId) {

        InvestmentTransaction transaction =
                transactionRepository.findByIdAndCustomerId(id, customerId)
                        .orElseThrow(() ->
                                new TransactionNotFoundException(
                                        "Transaction not found with ID: "
                                                + id));

        return new TransactionResponse(transaction);
    }

        public List<TransactionResponse> getAllTransactions(Integer customerId) {

        return transactionRepository
                                .findAllTransactionsByCustomerId(customerId)
                .stream()
                .map(TransactionResponse::new)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getTransactionsByInvestment(
                        Integer investmentId,
                        Integer customerId) {

                investmentRepository.findInvestmentByIdAndCustomerId(investmentId, customerId)
                .orElseThrow(() ->
                        new InvestmentNotFoundException(
                                "Investment not found with ID: "
                                        + investmentId));

        return transactionRepository
                .findByInvestmentIdAndCustomerId(investmentId, customerId)
                .stream()
                .map(TransactionResponse::new)
                .collect(Collectors.toList());
    }

    public void deleteTransaction(Integer id, Integer customerId) {

        transactionRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() ->
                        new TransactionNotFoundException(
                                "Transaction not found with ID: "
                                        + id));

        transactionRepository.deleteTransaction(id);
    }

        private Investment resolveImportedInvestment(
                        TabularDataService.TabularRow row,
                        List<Investment> investments,
                        Map<Integer, Investment> investmentsById,
                        Map<Integer, Portfolio> portfoliosById,
                        Map<String, List<Portfolio>> portfoliosByName) {

                String investmentIdText = optionalText(row, "investment_id");
                if (investmentIdText != null) {
                        Integer investmentId = positiveInteger(investmentIdText, "investment_id");
                        Investment investment = investmentsById.get(investmentId);
                        if (investment != null) {
                                return investment;
                        }
                        throw new IllegalArgumentException("investment_id does not belong to the current user.");
                }

                Integer portfolioId = resolveOptionalPortfolioId(row, portfoliosById, portfoliosByName);
                String symbol = optionalText(row, "symbol");
                String schemeCode = optionalText(row, "scheme_code");
                String companyName = optionalText(row, "company_name");

                if (portfolioId == null && symbol == null && schemeCode == null && companyName == null) {
                        throw new IllegalArgumentException(
                                        "Provide investment_id or enough investment details to resolve the transaction owner.");
                }

                List<Investment> matches = investments.stream()
                                .filter(investment -> portfolioId == null || portfolioId.equals(investment.getPortfolioId()))
                                .filter(investment -> schemeCode == null || equalsIgnoreCase(investment.getSchemeCode(), schemeCode))
                                .filter(investment -> symbol == null || equalsIgnoreCase(investment.getSymbol(), symbol))
                                .filter(investment -> companyName == null || equalsIgnoreCase(investment.getCompanyName(), companyName))
                                .collect(Collectors.toList());

                if (matches.isEmpty()) {
                        throw new IllegalArgumentException("Unable to match the row to one of the current user's investments.");
                }
                if (matches.size() > 1) {
                        throw new IllegalArgumentException(
                                        "Multiple investments matched this row. Add investment_id to disambiguate it.");
                }

                return matches.get(0);
        }

        private Integer resolveOptionalPortfolioId(
                        TabularDataService.TabularRow row,
                        Map<Integer, Portfolio> portfoliosById,
                        Map<String, List<Portfolio>> portfoliosByName) {

                String portfolioIdText = optionalText(row, "portfolio_id");
                if (portfolioIdText != null) {
                        Integer portfolioId = positiveInteger(portfolioIdText, "portfolio_id");
                        if (!portfoliosById.containsKey(portfolioId)) {
                                throw new IllegalArgumentException("portfolio_id does not belong to the current user.");
                        }
                        return portfolioId;
                }

                String portfolioName = optionalText(row, "portfolio_name");
                if (portfolioName == null) {
                        return null;
                }

                List<Portfolio> matches = portfoliosByName.getOrDefault(normalizeLookupKey(portfolioName), List.of());
                if (matches.isEmpty()) {
                        throw new IllegalArgumentException("portfolio_name was not found for the current user.");
                }
                if (matches.size() > 1) {
                        throw new IllegalArgumentException("portfolio_name is ambiguous. Use portfolio_id instead.");
                }
                return matches.get(0).getPortfolioId();
        }

        private boolean equalsIgnoreCase(String left, String right) {
                return normalizeLookupKey(left).equals(normalizeLookupKey(right));
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
}