package com.beantheredonethat.portfoliomanager.service;

import com.beantheredonethat.portfoliomanager.dto.CreateInvestmentRequest;
import com.beantheredonethat.portfoliomanager.dto.InvestmentResponse;
import com.beantheredonethat.portfoliomanager.dto.SymbolResolutionResult;
import com.beantheredonethat.portfoliomanager.dto.UpdateInvestmentRequest;
import com.beantheredonethat.portfoliomanager.entity.Investment;
import com.beantheredonethat.portfoliomanager.exception.InvestmentNotFoundException;
import com.beantheredonethat.portfoliomanager.exception.PortfolioNotFoundException;
import com.beantheredonethat.portfoliomanager.exception.YahooFinanceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.beantheredonethat.portfoliomanager.repository.InvestmentRepository;
import com.beantheredonethat.portfoliomanager.repository.PortfolioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvestmentService {

    private static final Logger logger = LoggerFactory.getLogger(InvestmentService.class);

    private final InvestmentRepository investmentRepository;
    private final PortfolioRepository portfolioRepository;
    private final YahooFinanceService yahooFinanceService;
    private final SymbolResolverService symbolResolverService;

    public InvestmentService(InvestmentRepository investmentRepository,
                             PortfolioRepository portfolioRepository,
                             YahooFinanceService yahooFinanceService,
                             SymbolResolverService symbolResolverService) {
        this.investmentRepository = investmentRepository;
        this.portfolioRepository = portfolioRepository;
        this.yahooFinanceService = yahooFinanceService;
        this.symbolResolverService = symbolResolverService;
    }

    public InvestmentResponse createInvestment(CreateInvestmentRequest request) {
        // Verify portfolio exists
        portfolioRepository.findById(request.getPortfolioId())
                .orElseThrow(() -> new PortfolioNotFoundException(
                        "Portfolio not found with ID: " + request.getPortfolioId()));

        String originalSymbol = request.getSymbol() == null ? null : request.getSymbol().trim().toUpperCase();
        SymbolResolutionResult resolution = symbolResolverService.resolveSymbol(originalSymbol, request.getAssetType());

        String resolvedSymbol = resolution.getResolvedSymbol() != null
                ? resolution.getResolvedSymbol()
                : originalSymbol;
        String resolvedExchange = resolution.getExchange();
        String resolvedCurrency = resolution.getCurrency();
        String resolvedName = resolution.getAssetName();

        // Calculate investedAmount
        BigDecimal investedAmount = request.getQuantity().multiply(request.getPurchasePrice()).setScale(2, RoundingMode.HALF_UP);

        // Keep manual current price as fallback; use market price when available.
        BigDecimal currentPrice = request.getCurrentPrice();
        BigDecimal currentValue = null;
        BigDecimal profitLoss = null;
        try {
            if (resolvedSymbol != null && !resolvedSymbol.isBlank()) {
                BigDecimal marketPrice = yahooFinanceService.getCurrentPrice(resolvedSymbol);
                if (marketPrice != null) {
                    currentPrice = marketPrice;
                }
            }
        } catch (Exception ex) {
            logger.warn("Failed to fetch market price for symbol {}: {}. Falling back to manual price.", resolvedSymbol, ex.getMessage());
        }

        if (currentPrice != null) {
            currentValue = currentPrice.multiply(request.getQuantity()).setScale(2, RoundingMode.HALF_UP);
            profitLoss = currentValue.subtract(investedAmount).setScale(2, RoundingMode.HALF_UP);
        }

        Investment investment = new Investment();
        investment.setPortfolioId(request.getPortfolioId());
        investment.setSymbol(resolvedSymbol);
        investment.setCompanyName((resolvedName != null && !resolvedName.isBlank()) ? resolvedName : request.getCompanyName());
        investment.setExchange(resolvedExchange);
        investment.setCurrency(resolvedCurrency);
        investment.setAssetType(request.getAssetType());
        investment.setCustomAssetType(request.getCustomAssetType());
        investment.setQuantity(request.getQuantity());
        investment.setInvestedAmount(investedAmount);
        investment.setPurchasePrice(request.getPurchasePrice());
        investment.setCurrentPrice(currentPrice);
        investment.setCurrentValue(currentValue);
        investment.setProfitLoss(profitLoss);
        investment.setPurchaseDate(request.getPurchaseDate());

        Investment saved = investmentRepository.insertInvestment(investment);
        return mapToInvestmentResponse(saved);
    }

    public InvestmentResponse getInvestmentById(Integer id) {
        Investment investment = investmentRepository.findInvestmentById(id)
                .orElseThrow(() -> new InvestmentNotFoundException("Investment not found with ID: " + id));

        refreshMarketValues(investment);
        // reload to get updated values (or assume repository updated investment object)
        Investment refreshed = investmentRepository.findInvestmentById(id).orElse(investment);
        return mapToInvestmentResponse(refreshed);
    }

    public List<InvestmentResponse> getAllInvestments() {
        List<Investment> investments = investmentRepository.findAllInvestments();
        for (Investment inv : investments) {
            try {
                refreshMarketValues(inv);
            } catch (YahooFinanceException yfe) {
                logger.warn("Failed to refresh market values for investment {}: {}", inv.getInvestmentId(), yfe.getMessage());
            } catch (Exception e) {
                logger.warn("Unexpected error refreshing market values for investment {}: {}", inv.getInvestmentId(), e.getMessage());
            }
        }
        return investments.stream().map(this::mapToInvestmentResponse).collect(Collectors.toList());
    }

    public List<InvestmentResponse> getInvestmentsByPortfolio(Integer portfolioId) {
        portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException("Portfolio not found with ID: " + portfolioId));

        List<Investment> investments = investmentRepository.findByPortfolioId(portfolioId);
        for (Investment inv : investments) {
            try {
                refreshMarketValues(inv);
            } catch (YahooFinanceException yfe) {
                logger.warn("Failed to refresh market values for investment {}: {}", inv.getInvestmentId(), yfe.getMessage());
            } catch (Exception e) {
                logger.warn("Unexpected error refreshing market values for investment {}: {}", inv.getInvestmentId(), e.getMessage());
            }
        }
        return investments.stream().map(this::mapToInvestmentResponse).collect(Collectors.toList());
    }

    public InvestmentResponse updateInvestment(Integer id, UpdateInvestmentRequest request) {
        Investment investment = investmentRepository.findInvestmentById(id)
                .orElseThrow(() -> new InvestmentNotFoundException("Investment not found with ID: " + id));

        if (request.getQuantity() != null) {
            investment.setQuantity(request.getQuantity());
        }
        if (request.getPurchasePrice() != null) {
            investment.setPurchasePrice(request.getPurchasePrice());
        }
        if (request.getPurchaseDate() != null) {
            investment.setPurchaseDate(request.getPurchaseDate());
        }
        if (request.getAssetType() != null) {
            investment.setAssetType(request.getAssetType());
        }
        if (request.getCustomAssetType() != null) {
            investment.setCustomAssetType(request.getCustomAssetType());
        }

        // Recalculate investedAmount
        if (investment.getQuantity() != null && investment.getPurchasePrice() != null) {
            investment.setInvestedAmount(investment.getQuantity().multiply(investment.getPurchasePrice()).setScale(2, RoundingMode.HALF_UP));
        }

        // Fetch latest price and recalc market values. If refresh fails, still save editable fields.
        try {
            refreshMarketValues(investment);
        } catch (YahooFinanceException yfe) {
            logger.warn("Failed to refresh market values for investment {}: {}", investment.getInvestmentId(), yfe.getMessage());
        }

        // Save editable fields + invested amount
        investmentRepository.updateInvestment(investment);

        Investment refreshed = investmentRepository.findInvestmentById(id).orElse(investment);
        return mapToInvestmentResponse(refreshed);
    }

    public void deleteInvestment(Integer id) {
        investmentRepository.findInvestmentById(id)
                .orElseThrow(() -> new InvestmentNotFoundException("Investment not found with ID: " + id));
        investmentRepository.deleteInvestment(id);
    }

    // Helper methods
    private void refreshMarketValues(Investment investment) {
        if (investment.getSymbol() == null) {
            return;
        }

        try {
            BigDecimal currentPrice = yahooFinanceService.getCurrentPrice(investment.getSymbol());
            BigDecimal currentValue = null;
            BigDecimal profitLoss = null;

            if (currentPrice != null && investment.getQuantity() != null) {
                currentValue = currentPrice.multiply(investment.getQuantity()).setScale(2, RoundingMode.HALF_UP);
                if (investment.getInvestedAmount() != null) {
                    profitLoss = currentValue.subtract(investment.getInvestedAmount()).setScale(2, RoundingMode.HALF_UP);
                }
            }

            // Persist refreshed values
            investmentRepository.updateMarketValues(investment.getInvestmentId(), currentPrice, currentValue, profitLoss);
            // Update in-memory object as well
            investment.setCurrentPrice(currentPrice);
            investment.setCurrentValue(currentValue);
            investment.setProfitLoss(profitLoss);
        } catch (YahooFinanceException yfe) {
            logger.warn("Unable to refresh market values for symbol {}: {}", investment.getSymbol(), yfe.getMessage());
            // Leave existing market values as-is (may be null)
        } catch (IllegalArgumentException iae) {
            logger.warn("Skipping refresh for unresolved symbol {}: {}", investment.getSymbol(), iae.getMessage());
        }
    }

    private InvestmentResponse mapToInvestmentResponse(Investment investment) {
        return new InvestmentResponse(investment);
    }
}

