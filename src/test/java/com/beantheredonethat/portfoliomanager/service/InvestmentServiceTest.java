package com.beantheredonethat.portfoliomanager.service;

import com.beantheredonethat.portfoliomanager.dto.CreateInvestmentRequest;
import com.beantheredonethat.portfoliomanager.dto.ExchangeRateResponse;
import com.beantheredonethat.portfoliomanager.dto.ImportSummaryResponse;
import com.beantheredonethat.portfoliomanager.dto.InvestmentResponse;
import com.beantheredonethat.portfoliomanager.dto.SymbolResolutionResult;
import com.beantheredonethat.portfoliomanager.dto.UpdateInvestmentRequest;
import com.beantheredonethat.portfoliomanager.entity.Investment;
import com.beantheredonethat.portfoliomanager.entity.Portfolio;
import com.beantheredonethat.portfoliomanager.exception.InvestmentNotFoundException;
import com.beantheredonethat.portfoliomanager.exception.PortfolioNotFoundException;
import com.beantheredonethat.portfoliomanager.marketdata.AssetType;
import com.beantheredonethat.portfoliomanager.marketdata.MarketDataFactory;
import com.beantheredonethat.portfoliomanager.marketdata.MarketDataResponse;
import com.beantheredonethat.portfoliomanager.marketdata.MarketDataService;
import com.beantheredonethat.portfoliomanager.repository.InvestmentRepository;
import com.beantheredonethat.portfoliomanager.repository.PortfolioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestmentServiceTest {

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private MarketDataFactory marketDataFactory;

    @Mock
    private CurrencyConversionService currencyConversionService;

    @Mock
    private SymbolResolverService symbolResolverService;

    @Mock
    private TabularDataService tabularDataService;

    @Mock
    private MarketDataService marketDataService;

    @InjectMocks
    private InvestmentService investmentService;

    @Test
    void createInvestment_success_savesWithComputedFields() {
        CreateInvestmentRequest request = buildCreateRequest(10, "AAPL");

        when(portfolioRepository.findByIdAndCustomerId(10, 1)).thenReturn(Optional.of(new Portfolio(10, 1, "Core")));
        when(symbolResolverService.resolveSymbol("AAPL", "STOCK"))
                .thenReturn(new SymbolResolutionResult("AAPL", "NASDAQ", "USD", "Apple Inc"));
        when(marketDataFactory.getService(AssetType.STOCK)).thenReturn(marketDataService);

        MarketDataResponse marketDataResponse = new MarketDataResponse();
        marketDataResponse.setPrice(new BigDecimal("120"));
        marketDataResponse.setCurrency("USD");
        when(marketDataService.getCurrentPrice(any())).thenReturn(marketDataResponse);

        when(investmentRepository.insertInvestment(any(Investment.class))).thenAnswer(invocation -> {
            Investment i = invocation.getArgument(0);
            i.setInvestmentId(200);
            return i;
        });

        InvestmentResponse response = investmentService.createInvestment(1, request);

        assertEquals(200, response.getInvestmentId());
        assertEquals(new BigDecimal("200.00"), response.getInvestedAmount());
        assertEquals(new BigDecimal("120.00"), response.getCurrentPrice());
        assertEquals(new BigDecimal("240.00"), response.getCurrentValue());
        assertEquals(new BigDecimal("40.00"), response.getProfitLoss());
        assertEquals("Apple Inc", response.getCompanyName());

        verify(investmentRepository).insertInvestment(any(Investment.class));
    }

    @Test
    void createInvestment_portfolioNotFound_throwsPortfolioNotFoundException() {
        when(portfolioRepository.findByIdAndCustomerId(10, 1)).thenReturn(Optional.empty());

        assertThrows(PortfolioNotFoundException.class,
                () -> investmentService.createInvestment(1, buildCreateRequest(10, "AAPL")));

        verify(investmentRepository, never()).insertInvestment(any(Investment.class));
    }

    @Test
    void getInvestmentById_notFound_throwsInvestmentNotFoundException() {
        when(investmentRepository.findInvestmentByIdAndCustomerId(50, 1)).thenReturn(Optional.empty());

        assertThrows(InvestmentNotFoundException.class,
                () -> investmentService.getInvestmentById(50, 1));
    }

    @Test
    void getAllInvestments_success_returnsMappedList() {
        Investment investment = buildInvestment(88, 10, "AAPL", "USD", "STOCK", "2", "100", "200");
        when(investmentRepository.findByCustomerId(1)).thenReturn(List.of(investment));
        when(marketDataFactory.getService(AssetType.STOCK)).thenReturn(marketDataService);

        MarketDataResponse marketDataResponse = new MarketDataResponse();
        marketDataResponse.setPrice(new BigDecimal("125"));
        marketDataResponse.setCurrency("USD");
        when(marketDataService.getCurrentPrice(any())).thenReturn(marketDataResponse);

        List<InvestmentResponse> responses = investmentService.getAllInvestments(1);

        assertEquals(1, responses.size());
        assertEquals(88, responses.get(0).getInvestmentId());
        verify(investmentRepository).updateMarketValues(eq(88), eq(new BigDecimal("125.00")), eq(new BigDecimal("250.00")), eq(new BigDecimal("50.00")));
    }

    @Test
    void getInvestmentsByPortfolio_portfolioMissing_throwsPortfolioNotFoundException() {
        when(portfolioRepository.findByIdAndCustomerId(10, 1)).thenReturn(Optional.empty());

        assertThrows(PortfolioNotFoundException.class,
                () -> investmentService.getInvestmentsByPortfolio(10, 1));
    }

    @Test
    void updateInvestment_success_recalculatesAndUpdates() {
        Investment existing = buildInvestment(77, 10, "AAPL", "USD", "STOCK", "10", "50", "500");
        when(investmentRepository.findInvestmentByIdAndCustomerId(77, 1)).thenReturn(Optional.of(existing));
        when(marketDataFactory.getService(AssetType.STOCK)).thenReturn(marketDataService);

        MarketDataResponse marketDataResponse = new MarketDataResponse();
        marketDataResponse.setPrice(new BigDecimal("60"));
        marketDataResponse.setCurrency("USD");
        when(marketDataService.getCurrentPrice(any())).thenReturn(marketDataResponse);

        UpdateInvestmentRequest request = new UpdateInvestmentRequest();
        request.setQuantity(new BigDecimal("12"));
        request.setPurchasePrice(new BigDecimal("55"));
        request.setSchemeCode("SCHEME-1");

        InvestmentResponse response = investmentService.updateInvestment(77, 1, request);

        assertEquals(new BigDecimal("660.00"), response.getInvestedAmount());
        assertEquals("SCHEME-1", response.getSchemeCode());
        verify(investmentRepository).updateInvestment(existing);
        verify(investmentRepository).updateMarketValues(eq(77), eq(new BigDecimal("60.00")), eq(new BigDecimal("720.00")), eq(new BigDecimal("60.00")));
    }

    @Test
    void deleteInvestment_success_deletesById() {
        when(investmentRepository.findInvestmentByIdAndCustomerId(90, 1)).thenReturn(Optional.of(buildInvestment(90, 10, "AAPL", "USD", "STOCK", "1", "100", "100")));

        investmentService.deleteInvestment(90, 1);

        verify(investmentRepository).deleteInvestment(90);
    }

    @Test
    void exportInvestmentsCsv_success_writesRowsWithHeaders() {
        when(portfolioRepository.findByCustomerId(1)).thenReturn(List.of(new Portfolio(10, 1, "Core")));
        when(investmentRepository.findByCustomerId(1)).thenReturn(List.of(buildInvestment(88, 10, "AAPL", "USD", "STOCK", "2", "100", "200")));
        when(tabularDataService.writeCsv(any(), any())).thenReturn("csv-content");

        String csv = investmentService.exportInvestmentsCsv(1);

        assertEquals("csv-content", csv);
        verify(tabularDataService).writeCsv(any(), any());
    }

    @Test
    void importInvestments_mixedRows_returnsSummaryCounts() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);

        List<String> headers = List.of(
                "investment_id", "portfolio_id", "portfolio_name", "symbol", "scheme_code", "company_name",
                "exchange", "currency", "asset_type", "custom_asset_type", "quantity", "invested_amount",
                "purchase_price", "current_price", "current_value", "profit_loss", "purchase_date");

        Map<String, String> valid = new LinkedHashMap<>();
        valid.put("portfolio_id", "10");
        valid.put("portfolio_name", "Core");
        valid.put("symbol", "AAPL");
        valid.put("scheme_code", "");
        valid.put("company_name", "Apple");
        valid.put("exchange", "NASDAQ");
        valid.put("currency", "USD");
        valid.put("asset_type", "STOCK");
        valid.put("custom_asset_type", "");
        valid.put("quantity", "2");
        valid.put("invested_amount", "");
        valid.put("purchase_price", "100");
        valid.put("current_price", "120");
        valid.put("current_value", "");
        valid.put("profit_loss", "");
        valid.put("purchase_date", "2025-01-01");

        Map<String, String> invalid = new LinkedHashMap<>(valid);
        invalid.put("portfolio_id", "999");

        TabularDataService.TabularRow row1 = new TabularDataService.TabularRow(2, valid);
        TabularDataService.TabularRow row2 = new TabularDataService.TabularRow(3, invalid);
        TabularDataService.ParsedTabularData parsed = new TabularDataService.ParsedTabularData(headers, List.of(row1, row2));

        when(tabularDataService.read(file)).thenReturn(parsed);
        when(portfolioRepository.findByCustomerId(1)).thenReturn(List.of(new Portfolio(10, 1, "Core")));
        when(portfolioRepository.findByIdAndCustomerId(10, 1)).thenReturn(Optional.of(new Portfolio(10, 1, "Core")));

        when(symbolResolverService.resolveSymbol("AAPL", "STOCK"))
                .thenReturn(new SymbolResolutionResult("AAPL", "NASDAQ", "USD", "Apple Inc"));
        when(marketDataFactory.getService(AssetType.STOCK)).thenReturn(marketDataService);

        MarketDataResponse marketDataResponse = new MarketDataResponse();
        marketDataResponse.setPrice(new BigDecimal("120"));
        marketDataResponse.setCurrency("USD");
        when(marketDataService.getCurrentPrice(any())).thenReturn(marketDataResponse);

        when(investmentRepository.insertInvestment(any(Investment.class))).thenAnswer(invocation -> {
            Investment i = invocation.getArgument(0);
            i.setInvestmentId(501);
            return i;
        });

        ImportSummaryResponse response = investmentService.importInvestments(1, file);

        assertEquals(1, response.getSuccessfulCount());
        assertEquals(1, response.getFailedCount());
        assertEquals(1, response.getFailures().size());
        verify(investmentRepository).insertInvestment(any(Investment.class));
    }

    @Test
    void getExchangeRate_success_normalizesCurrencyCodes() {
        when(currencyConversionService.getExchangeRate("USD", "INR")).thenReturn(new BigDecimal("83.12345678"));

        ExchangeRateResponse response = investmentService.getExchangeRate(" usd ", "inr ");

        assertEquals("USD", response.getFrom());
        assertEquals("INR", response.getTo());
        assertEquals(new BigDecimal("83.12345678"), response.getRate());
    }

    private CreateInvestmentRequest buildCreateRequest(Integer portfolioId, String symbol) {
        CreateInvestmentRequest request = new CreateInvestmentRequest();
        request.setPortfolioId(portfolioId);
        request.setSymbol(symbol);
        request.setCompanyName("Apple");
        request.setAssetType("STOCK");
        request.setQuantity(new BigDecimal("2"));
        request.setPurchasePrice(new BigDecimal("100"));
        request.setPurchaseDate(java.time.LocalDate.of(2025, 1, 1));
        request.setCurrency("usd");
        return request;
    }

    private Investment buildInvestment(Integer id,
                                       Integer portfolioId,
                                       String symbol,
                                       String currency,
                                       String assetType,
                                       String quantity,
                                       String purchasePrice,
                                       String investedAmount) {
        Investment investment = new Investment();
        investment.setInvestmentId(id);
        investment.setPortfolioId(portfolioId);
        investment.setSymbol(symbol);
        investment.setCompanyName("Apple");
        investment.setExchange("NASDAQ");
        investment.setCurrency(currency);
        investment.setAssetType(assetType);
        investment.setQuantity(new BigDecimal(quantity));
        investment.setPurchasePrice(new BigDecimal(purchasePrice));
        investment.setInvestedAmount(new BigDecimal(investedAmount));
        investment.setPurchaseDate(java.time.LocalDate.of(2025, 1, 1));
        return investment;
    }
}
