package com.beantheredonethat.portfoliomanager.service;

import com.beantheredonethat.portfoliomanager.dto.RiskAnalysisResponse;
import com.beantheredonethat.portfoliomanager.entity.Investment;
import com.beantheredonethat.portfoliomanager.exception.PortfolioNotFoundException;
import com.beantheredonethat.portfoliomanager.exception.YahooFinanceException;
import com.beantheredonethat.portfoliomanager.repository.InvestmentRepository;
import com.beantheredonethat.portfoliomanager.repository.PortfolioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RiskAnalysisService {

    private static final String HISTORY_RANGE = "5y";
    private static final double TRADING_DAYS_PER_YEAR = 252.0d;
    private static final MathContext DIVISION_CONTEXT = new MathContext(12, RoundingMode.HALF_UP);

    private final YahooFinanceService yahooFinanceService;
    private final PortfolioRepository portfolioRepository;
    private final InvestmentRepository investmentRepository;
    private final BigDecimal riskFreeRate;

    public RiskAnalysisService(
            YahooFinanceService yahooFinanceService,
            PortfolioRepository portfolioRepository,
            InvestmentRepository investmentRepository,
            @Value("${risk.analysis.risk-free-rate:0.04}") BigDecimal riskFreeRate) {
        this.yahooFinanceService = yahooFinanceService;
        this.portfolioRepository = portfolioRepository;
        this.investmentRepository = investmentRepository;
        this.riskFreeRate = riskFreeRate;
    }

    public RiskAnalysisResponse analyzeStock(String symbol) {
        List<YahooFinanceService.HistoricalPricePoint> historicalPrices = yahooFinanceService.getHistoricalPrices(symbol, HISTORY_RANGE);
        List<Double> dailyReturns = buildDailyReturns(historicalPrices);
        AnalysisMetrics metrics = calculateMetrics(dailyReturns);

        RiskAnalysisResponse response = new RiskAnalysisResponse();
        response.setSymbol(symbol.trim().toUpperCase());
        response.setAnnualizedVolatility(toPercentage(metrics.annualizedVolatility()));
        response.setMaximumDrawdown(toPercentage(metrics.maximumDrawdown()));
        response.setAverageAnnualReturn(toPercentage(metrics.averageAnnualReturn()));
        response.setSharpeRatio(toDecimal(metrics.sharpeRatio(), 4));
        response.setRiskLevel(determineRiskLevel(response.getAnnualizedVolatility()));
        return response;
    }

    public RiskAnalysisResponse analyzePortfolio(Integer portfolioId, Integer customerId) {
        portfolioRepository.findByIdAndCustomerId(portfolioId, customerId)
                .orElseThrow(() -> new PortfolioNotFoundException("Portfolio not found with ID: " + portfolioId));

        List<Investment> investments = investmentRepository.findByPortfolioIdAndCustomerId(portfolioId, customerId);
        if (investments.isEmpty()) {
            throw new IllegalArgumentException("Portfolio has no investments to analyze: " + portfolioId);
        }

        List<HoldingAnalysisInput> holdings = new ArrayList<>();
        BigDecimal totalPortfolioValue = BigDecimal.ZERO;

        for (Investment investment : investments) {
            validateHolding(investment);

            BigDecimal currentPrice = yahooFinanceService.getCurrentPrice(investment.getSymbol());
            BigDecimal currentValue = currentPrice.multiply(investment.getQuantity()).setScale(2, RoundingMode.HALF_UP);
            List<YahooFinanceService.HistoricalPricePoint> historicalPrices = yahooFinanceService.getHistoricalPrices(investment.getSymbol(), HISTORY_RANGE);

            holdings.add(new HoldingAnalysisInput(investment.getSymbol().trim().toUpperCase(), currentValue, historicalPrices));
            totalPortfolioValue = totalPortfolioValue.add(currentValue);
        }

        if (totalPortfolioValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Portfolio value must be greater than zero for risk analysis");
        }

        List<Double> weightedDailyReturns = buildWeightedPortfolioReturns(holdings, totalPortfolioValue);
        AnalysisMetrics metrics = calculateMetrics(weightedDailyReturns);

        RiskAnalysisResponse response = new RiskAnalysisResponse();
        response.setPortfolioId(portfolioId);
        response.setPortfolioValue(totalPortfolioValue.setScale(2, RoundingMode.HALF_UP));
        response.setAnnualizedVolatility(toPercentage(metrics.annualizedVolatility()));
        response.setMaximumDrawdown(toPercentage(metrics.maximumDrawdown()));
        response.setAverageAnnualReturn(toPercentage(metrics.averageAnnualReturn()));
        response.setSharpeRatio(toDecimal(metrics.sharpeRatio(), 4));
        response.setRiskLevel(determineRiskLevel(response.getAnnualizedVolatility()));
        return response;
    }

    private void validateHolding(Investment investment) {
        if (investment.getSymbol() == null || investment.getSymbol().trim().isEmpty()) {
            throw new IllegalArgumentException("Investment " + investment.getInvestmentId() + " is missing a ticker symbol");
        }
        if (investment.getQuantity() == null || investment.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Investment " + investment.getInvestmentId() + " must have a positive quantity");
        }
    }

    private List<Double> buildWeightedPortfolioReturns(List<HoldingAnalysisInput> holdings, BigDecimal totalPortfolioValue) {
        Map<String, Map<LocalDate, Double>> holdingReturnsBySymbol = new HashMap<>();
        Set<LocalDate> commonDates = null;

        for (HoldingAnalysisInput holding : holdings) {
            Map<LocalDate, Double> holdingReturns = buildDailyReturnMap(holding.historicalPrices());
            if (holdingReturns.size() < 2) {
                throw new YahooFinanceException("Missing historical data for symbol: " + holding.symbol());
            }

            holdingReturnsBySymbol.put(holding.symbol(), holdingReturns);
            if (commonDates == null) {
                commonDates = new HashSet<>(holdingReturns.keySet());
            } else {
                commonDates.retainAll(holdingReturns.keySet());
            }
        }

        if (commonDates == null || commonDates.size() < 2) {
            throw new YahooFinanceException("Missing overlapping historical data for portfolio analysis");
        }

        List<LocalDate> orderedDates = commonDates.stream().sorted().collect(Collectors.toList());
        List<Double> weightedDailyReturns = new ArrayList<>();

        for (LocalDate tradingDate : orderedDates) {
            double portfolioReturn = 0.0d;
            for (HoldingAnalysisInput holding : holdings) {
                BigDecimal weight = holding.currentValue().divide(totalPortfolioValue, DIVISION_CONTEXT);
                Double holdingReturn = holdingReturnsBySymbol.get(holding.symbol()).get(tradingDate);
                portfolioReturn += weight.doubleValue() * holdingReturn;
            }
            weightedDailyReturns.add(portfolioReturn);
        }

        return weightedDailyReturns;
    }

    private Map<LocalDate, Double> buildDailyReturnMap(List<YahooFinanceService.HistoricalPricePoint> historicalPrices) {
        Map<LocalDate, Double> dailyReturns = new HashMap<>();
        for (int index = 1; index < historicalPrices.size(); index++) {
            BigDecimal previousClose = historicalPrices.get(index - 1).getClosePrice();
            BigDecimal currentClose = historicalPrices.get(index).getClosePrice();

            if (previousClose == null || currentClose == null || previousClose.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Daily return = (current close - previous close) / previous close.
            double dailyReturn = currentClose.subtract(previousClose)
                    .divide(previousClose, DIVISION_CONTEXT)
                    .doubleValue();
            dailyReturns.put(historicalPrices.get(index).getTradingDate(), dailyReturn);
        }
        return dailyReturns;
    }

    private List<Double> buildDailyReturns(List<YahooFinanceService.HistoricalPricePoint> historicalPrices) {
        List<Double> dailyReturns = new ArrayList<>();
        for (int index = 1; index < historicalPrices.size(); index++) {
            BigDecimal previousClose = historicalPrices.get(index - 1).getClosePrice();
            BigDecimal currentClose = historicalPrices.get(index).getClosePrice();

            if (previousClose == null || currentClose == null || previousClose.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            dailyReturns.add(currentClose.subtract(previousClose)
                    .divide(previousClose, DIVISION_CONTEXT)
                    .doubleValue());
        }

        if (dailyReturns.size() < 2) {
            throw new YahooFinanceException("Missing historical data to calculate risk metrics");
        }
        return dailyReturns;
    }

    private AnalysisMetrics calculateMetrics(List<Double> dailyReturns) {
        double annualizedVolatility = calculateAnnualizedVolatility(dailyReturns);
        double maximumDrawdown = calculateMaximumDrawdown(dailyReturns);
        double averageAnnualReturn = calculateAverageAnnualReturn(dailyReturns);
        double sharpeRatio = annualizedVolatility == 0.0d
                ? 0.0d
                : (averageAnnualReturn - riskFreeRate.doubleValue()) / annualizedVolatility;

        return new AnalysisMetrics(annualizedVolatility, maximumDrawdown, averageAnnualReturn, sharpeRatio);
    }

    private double calculateAnnualizedVolatility(List<Double> dailyReturns) {
        double meanDailyReturn = dailyReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0d);
        double squaredDeviationSum = 0.0d;
        for (double dailyReturn : dailyReturns) {
            squaredDeviationSum += Math.pow(dailyReturn - meanDailyReturn, 2);
        }

        // Annualized volatility = standard deviation of daily returns * sqrt(252).
        double variance = squaredDeviationSum / (dailyReturns.size() - 1);
        return Math.sqrt(variance) * Math.sqrt(TRADING_DAYS_PER_YEAR);
    }

    private double calculateMaximumDrawdown(List<Double> dailyReturns) {
        double cumulativeValue = 1.0d;
        double peakValue = 1.0d;
        double maximumDrawdown = 0.0d;

        for (double dailyReturn : dailyReturns) {
            cumulativeValue *= (1.0d + dailyReturn);
            peakValue = Math.max(peakValue, cumulativeValue);

            // Drawdown = (current value - previous peak) / previous peak.
            double currentDrawdown = (cumulativeValue - peakValue) / peakValue;
            maximumDrawdown = Math.min(maximumDrawdown, currentDrawdown);
        }

        return maximumDrawdown;
    }

    private double calculateAverageAnnualReturn(List<Double> dailyReturns) {
        double cumulativeValue = 1.0d;
        for (double dailyReturn : dailyReturns) {
            cumulativeValue *= (1.0d + dailyReturn);
        }

        // Annualized return compounds the observed cumulative return over 252 trading days.
        return Math.pow(cumulativeValue, TRADING_DAYS_PER_YEAR / dailyReturns.size()) - 1.0d;
    }

    private BigDecimal toPercentage(double ratioValue) {
        return BigDecimal.valueOf(ratioValue)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal toDecimal(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }

    private String determineRiskLevel(BigDecimal volatility) {

        if (volatility.compareTo(BigDecimal.valueOf(20)) < 0) {
            return "LOW";
        }

        if (volatility.compareTo(BigDecimal.valueOf(30)) < 0) {
            return "MODERATE";
        }

        if (volatility.compareTo(BigDecimal.valueOf(45)) < 0) {
            return "HIGH";
        }

        return "VERY HIGH";
    }

    private static class HoldingAnalysisInput {
        private final String symbol;
        private final BigDecimal currentValue;
        private final List<YahooFinanceService.HistoricalPricePoint> historicalPrices;

        private HoldingAnalysisInput(String symbol, BigDecimal currentValue, List<YahooFinanceService.HistoricalPricePoint> historicalPrices) {
            this.symbol = symbol;
            this.currentValue = currentValue;
            this.historicalPrices = historicalPrices;
        }

        public String symbol() {
            return symbol;
        }

        public BigDecimal currentValue() {
            return currentValue;
        }

        public List<YahooFinanceService.HistoricalPricePoint> historicalPrices() {
            return historicalPrices;
        }
    }

    private static class AnalysisMetrics {
        private final double annualizedVolatility;
        private final double maximumDrawdown;
        private final double averageAnnualReturn;
        private final double sharpeRatio;

        private AnalysisMetrics(double annualizedVolatility, double maximumDrawdown, double averageAnnualReturn, double sharpeRatio) {
            this.annualizedVolatility = annualizedVolatility;
            this.maximumDrawdown = maximumDrawdown;
            this.averageAnnualReturn = averageAnnualReturn;
            this.sharpeRatio = sharpeRatio;
        }

        public double annualizedVolatility() {
            return annualizedVolatility;
        }

        public double maximumDrawdown() {
            return maximumDrawdown;
        }

        public double averageAnnualReturn() {
            return averageAnnualReturn;
        }

        public double sharpeRatio() {
            return sharpeRatio;
        }
    }
}