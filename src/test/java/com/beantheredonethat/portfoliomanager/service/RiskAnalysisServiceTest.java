package com.beantheredonethat.portfoliomanager.service;

import com.beantheredonethat.portfoliomanager.dto.RiskAnalysisResponse;
import com.beantheredonethat.portfoliomanager.entity.Investment;
import com.beantheredonethat.portfoliomanager.entity.Portfolio;
import com.beantheredonethat.portfoliomanager.exception.PortfolioNotFoundException;
import com.beantheredonethat.portfoliomanager.exception.YahooFinanceException;
import com.beantheredonethat.portfoliomanager.repository.InvestmentRepository;
import com.beantheredonethat.portfoliomanager.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskAnalysisServiceTest {

    @Mock
    private YahooFinanceService yahooFinanceService;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private InvestmentRepository investmentRepository;

    @InjectMocks
    private RiskAnalysisService riskAnalysisService;

    @BeforeEach
    void setUp() {
        riskAnalysisService = new RiskAnalysisService(
                yahooFinanceService,
                portfolioRepository,
                investmentRepository,
                new BigDecimal("0.04")
        );
    }

    @Test
    void analyzeStock_success_returnsComputedMetrics() {
        when(yahooFinanceService.getHistoricalPrices(" aapl ", "5y")).thenReturn(priceSeries(
                LocalDate.of(2025, 1, 1), new BigDecimal("100"),
                LocalDate.of(2025, 1, 2), new BigDecimal("102"),
                LocalDate.of(2025, 1, 3), new BigDecimal("101"),
                LocalDate.of(2025, 1, 4), new BigDecimal("104")
        ));

        RiskAnalysisResponse response = riskAnalysisService.analyzeStock(" aapl ");

        assertEquals("AAPL", response.getSymbol());
        assertNotNull(response.getAnnualizedVolatility());
        assertNotNull(response.getMaximumDrawdown());
        assertNotNull(response.getAverageAnnualReturn());
        assertNotNull(response.getSharpeRatio());
        assertNotNull(response.getRiskLevel());
    }

    @Test
    void analyzeStock_insufficientData_throwsYahooFinanceException() {
        when(yahooFinanceService.getHistoricalPrices("AAPL", "5y")).thenReturn(List.of(
                new YahooFinanceService.HistoricalPricePoint(LocalDate.of(2025, 1, 1), new BigDecimal("100"))
        ));

        assertThrows(YahooFinanceException.class, () -> riskAnalysisService.analyzeStock("AAPL"));
    }

    @Test
    void analyzePortfolio_success_returnsPortfolioMetrics() {
        Integer portfolioId = 10;
        Integer customerId = 1;

        when(portfolioRepository.findByIdAndCustomerId(portfolioId, customerId))
                .thenReturn(Optional.of(new Portfolio(portfolioId, customerId, "Core")));

        Investment inv1 = buildInvestment(11, portfolioId, "AAPL", "2");
        Investment inv2 = buildInvestment(12, portfolioId, "MSFT", "4");
        when(investmentRepository.findByPortfolioIdAndCustomerId(portfolioId, customerId))
                .thenReturn(List.of(inv1, inv2));

        when(yahooFinanceService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("100"));
        when(yahooFinanceService.getCurrentPrice("MSFT")).thenReturn(new BigDecimal("50"));

        when(yahooFinanceService.getHistoricalPrices("AAPL", "5y")).thenReturn(priceSeries(
                LocalDate.of(2025, 1, 1), new BigDecimal("100"),
                LocalDate.of(2025, 1, 2), new BigDecimal("101"),
                LocalDate.of(2025, 1, 3), new BigDecimal("103"),
                LocalDate.of(2025, 1, 4), new BigDecimal("102")
        ));

        when(yahooFinanceService.getHistoricalPrices("MSFT", "5y")).thenReturn(priceSeries(
                LocalDate.of(2025, 1, 1), new BigDecimal("50"),
                LocalDate.of(2025, 1, 2), new BigDecimal("50.5"),
                LocalDate.of(2025, 1, 3), new BigDecimal("51"),
                LocalDate.of(2025, 1, 4), new BigDecimal("52")
        ));

        RiskAnalysisResponse response = riskAnalysisService.analyzePortfolio(portfolioId, customerId);

        assertEquals(portfolioId, response.getPortfolioId());
        assertEquals(new BigDecimal("400.00"), response.getPortfolioValue());
        assertNotNull(response.getAnnualizedVolatility());
        assertNotNull(response.getRiskLevel());

        verify(investmentRepository).findByPortfolioIdAndCustomerId(portfolioId, customerId);
    }

    @Test
    void analyzePortfolio_portfolioNotFound_throwsPortfolioNotFoundException() {
        when(portfolioRepository.findByIdAndCustomerId(10, 1)).thenReturn(Optional.empty());

        assertThrows(PortfolioNotFoundException.class, () -> riskAnalysisService.analyzePortfolio(10, 1));
    }

    @Test
    void analyzePortfolio_emptyInvestments_throwsIllegalArgumentException() {
        when(portfolioRepository.findByIdAndCustomerId(10, 1)).thenReturn(Optional.of(new Portfolio(10, 1, "Core")));
        when(investmentRepository.findByPortfolioIdAndCustomerId(10, 1)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> riskAnalysisService.analyzePortfolio(10, 1));
    }

    @Test
    void analyzePortfolio_invalidHolding_throwsIllegalArgumentException() {
        when(portfolioRepository.findByIdAndCustomerId(10, 1)).thenReturn(Optional.of(new Portfolio(10, 1, "Core")));

        Investment invalid = buildInvestment(11, 10, "", "2");
        when(investmentRepository.findByPortfolioIdAndCustomerId(10, 1)).thenReturn(List.of(invalid));

        assertThrows(IllegalArgumentException.class, () -> riskAnalysisService.analyzePortfolio(10, 1));
    }

    @Test
    void analyzePortfolio_nonOverlappingHistory_throwsYahooFinanceException() {
        when(portfolioRepository.findByIdAndCustomerId(10, 1)).thenReturn(Optional.of(new Portfolio(10, 1, "Core")));

        Investment inv1 = buildInvestment(11, 10, "AAPL", "2");
        Investment inv2 = buildInvestment(12, 10, "MSFT", "4");
        when(investmentRepository.findByPortfolioIdAndCustomerId(10, 1)).thenReturn(List.of(inv1, inv2));

        when(yahooFinanceService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("100"));
        when(yahooFinanceService.getCurrentPrice("MSFT")).thenReturn(new BigDecimal("50"));

        when(yahooFinanceService.getHistoricalPrices("AAPL", "5y")).thenReturn(priceSeries(
                LocalDate.of(2025, 1, 1), new BigDecimal("100"),
                LocalDate.of(2025, 1, 2), new BigDecimal("101"),
                LocalDate.of(2025, 1, 3), new BigDecimal("102")
        ));
        when(yahooFinanceService.getHistoricalPrices("MSFT", "5y")).thenReturn(priceSeries(
                LocalDate.of(2025, 2, 1), new BigDecimal("50"),
                LocalDate.of(2025, 2, 2), new BigDecimal("51"),
                LocalDate.of(2025, 2, 3), new BigDecimal("52")
        ));

        assertThrows(YahooFinanceException.class, () -> riskAnalysisService.analyzePortfolio(10, 1));
    }

    private Investment buildInvestment(Integer id, Integer portfolioId, String symbol, String quantity) {
        Investment investment = new Investment();
        investment.setInvestmentId(id);
        investment.setPortfolioId(portfolioId);
        investment.setSymbol(symbol);
        investment.setQuantity(new BigDecimal(quantity));
        return investment;
    }

    private List<YahooFinanceService.HistoricalPricePoint> priceSeries(
            LocalDate d1, BigDecimal p1,
            LocalDate d2, BigDecimal p2,
            LocalDate d3, BigDecimal p3) {
        return List.of(
                new YahooFinanceService.HistoricalPricePoint(d1, p1),
                new YahooFinanceService.HistoricalPricePoint(d2, p2),
                new YahooFinanceService.HistoricalPricePoint(d3, p3)
        );
    }

    private List<YahooFinanceService.HistoricalPricePoint> priceSeries(
            LocalDate d1, BigDecimal p1,
            LocalDate d2, BigDecimal p2,
            LocalDate d3, BigDecimal p3,
            LocalDate d4, BigDecimal p4) {
        return List.of(
                new YahooFinanceService.HistoricalPricePoint(d1, p1),
                new YahooFinanceService.HistoricalPricePoint(d2, p2),
                new YahooFinanceService.HistoricalPricePoint(d3, p3),
                new YahooFinanceService.HistoricalPricePoint(d4, p4)
        );
    }
}
