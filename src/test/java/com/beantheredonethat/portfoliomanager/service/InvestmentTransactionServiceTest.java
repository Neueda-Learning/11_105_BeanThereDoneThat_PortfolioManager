package com.beantheredonethat.portfoliomanager.service;

import com.beantheredonethat.portfoliomanager.dto.CreateTransactionRequest;
import com.beantheredonethat.portfoliomanager.dto.ImportSummaryResponse;
import com.beantheredonethat.portfoliomanager.dto.TransactionResponse;
import com.beantheredonethat.portfoliomanager.entity.Investment;
import com.beantheredonethat.portfoliomanager.entity.InvestmentTransaction;
import com.beantheredonethat.portfoliomanager.entity.Portfolio;
import com.beantheredonethat.portfoliomanager.exception.InvestmentNotFoundException;
import com.beantheredonethat.portfoliomanager.exception.TransactionNotFoundException;
import com.beantheredonethat.portfoliomanager.repository.InvestmentRepository;
import com.beantheredonethat.portfoliomanager.repository.InvestmentTransactionRepository;
import com.beantheredonethat.portfoliomanager.repository.PortfolioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestmentTransactionServiceTest {

    @Mock
    private InvestmentTransactionRepository transactionRepository;

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private TabularDataService tabularDataService;

    @InjectMocks
    private InvestmentTransactionService transactionService;

    @Test
    void createTransaction_buy_success_updatesInvestmentAndInsertsTransaction() {
        Investment investment = buildInvestment(11, 10, "AAPL", "USD", "STOCK", "5", "100");
        when(investmentRepository.findInvestmentByIdAndCustomerId(11, 1)).thenReturn(Optional.of(investment));
        when(transactionRepository.insertTransaction(any(InvestmentTransaction.class))).thenAnswer(invocation -> {
            InvestmentTransaction tx = invocation.getArgument(0);
            tx.setTransactionId(1001);
            return tx;
        });

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setInvestmentId(11);
        request.setTransactionType("buy");
        request.setQuantity(new BigDecimal("2"));
        request.setTransactionPrice(new BigDecimal("120"));
        request.setTransactionDate(LocalDate.of(2025, 1, 10));

        TransactionResponse response = transactionService.createTransaction(1, request);

        assertEquals(1001, response.getTransactionId());
        assertEquals("BUY", response.getTransactionType());
        assertEquals(new BigDecimal("240.00"), response.getTransactionAmount());

        verify(investmentRepository).updateInvestment(investment);
        verify(transactionRepository).insertTransaction(any(InvestmentTransaction.class));
    }

    @Test
    void createTransaction_sellInsufficientQuantity_throwsRuntimeException() {
        Investment investment = buildInvestment(11, 10, "AAPL", "USD", "STOCK", "1", "100");
        when(investmentRepository.findInvestmentByIdAndCustomerId(11, 1)).thenReturn(Optional.of(investment));

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setInvestmentId(11);
        request.setTransactionType("SELL");
        request.setQuantity(new BigDecimal("3"));
        request.setTransactionPrice(new BigDecimal("120"));
        request.setTransactionDate(LocalDate.of(2025, 1, 10));

        assertThrows(RuntimeException.class, () -> transactionService.createTransaction(1, request));

        verify(transactionRepository, never()).insertTransaction(any(InvestmentTransaction.class));
    }

    @Test
    void createTransaction_invalidType_throwsRuntimeException() {
        Investment investment = buildInvestment(11, 10, "AAPL", "USD", "STOCK", "5", "100");
        when(investmentRepository.findInvestmentByIdAndCustomerId(11, 1)).thenReturn(Optional.of(investment));

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setInvestmentId(11);
        request.setTransactionType("TRANSFER");
        request.setQuantity(new BigDecimal("1"));
        request.setTransactionPrice(new BigDecimal("120"));
        request.setTransactionDate(LocalDate.of(2025, 1, 10));

        assertThrows(RuntimeException.class, () -> transactionService.createTransaction(1, request));
    }

    @Test
    void getTransactionById_notFound_throwsTransactionNotFoundException() {
        when(transactionRepository.findByIdAndCustomerId(77, 1)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionService.getTransactionById(77, 1));
    }

    @Test
    void getAllTransactions_success_returnsMappedList() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.setTransactionId(5);
        tx.setInvestmentId(11);
        tx.setSymbol("AAPL");
        tx.setCompanyName("Apple");
        tx.setAssetType("STOCK");
        tx.setCurrency("USD");
        tx.setTransactionDate(LocalDate.of(2025, 1, 1));
        tx.setTransactionType("BUY");
        tx.setQuantity(new BigDecimal("1"));
        tx.setTransactionPrice(new BigDecimal("100"));
        tx.setTransactionAmount(new BigDecimal("100"));

        when(transactionRepository.findAllTransactionsByCustomerId(1)).thenReturn(List.of(tx));

        List<TransactionResponse> responses = transactionService.getAllTransactions(1);

        assertEquals(1, responses.size());
        assertEquals(5, responses.get(0).getTransactionId());
    }

    @Test
    void getTransactionsByInvestment_investmentMissing_throwsInvestmentNotFoundException() {
        when(investmentRepository.findInvestmentByIdAndCustomerId(11, 1)).thenReturn(Optional.empty());

        assertThrows(InvestmentNotFoundException.class, () -> transactionService.getTransactionsByInvestment(11, 1));
    }

    @Test
    void deleteTransaction_success_deletesById() {
        when(transactionRepository.findByIdAndCustomerId(5, 1)).thenReturn(Optional.of(new InvestmentTransaction()));

        transactionService.deleteTransaction(5, 1);

        verify(transactionRepository).deleteTransaction(5);
    }

    @Test
    void exportTransactionsCsv_success_delegatesToTabularDataService() {
        when(investmentRepository.findByCustomerId(1)).thenReturn(List.of(buildInvestment(11, 10, "AAPL", "USD", "STOCK", "5", "100")));
        when(portfolioRepository.findByCustomerId(1)).thenReturn(List.of(new Portfolio(10, 1, "Core")));

        InvestmentTransaction tx = new InvestmentTransaction();
        tx.setTransactionId(50);
        tx.setInvestmentId(11);
        tx.setSymbol("AAPL");
        tx.setCompanyName("Apple");
        tx.setAssetType("STOCK");
        tx.setCurrency("USD");
        tx.setTransactionDate(LocalDate.of(2025, 1, 1));
        tx.setTransactionType("BUY");
        tx.setQuantity(new BigDecimal("1"));
        tx.setTransactionPrice(new BigDecimal("100"));
        tx.setTransactionAmount(new BigDecimal("100"));

        when(transactionRepository.findByCustomerId(1)).thenReturn(List.of(tx));
        when(tabularDataService.writeCsv(any(), any())).thenReturn("csv");

        String csv = transactionService.exportTransactionsCsv(1);

        assertEquals("csv", csv);
        verify(tabularDataService).writeCsv(any(), any());
    }

    @Test
    void importTransactions_mixedRows_returnsSummaryCounts() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);

        List<String> headers = List.of(
                "transaction_id", "investment_id", "portfolio_id", "portfolio_name", "symbol", "scheme_code",
                "company_name", "asset_type", "transaction_date", "transaction_type", "quantity",
                "transaction_price", "transaction_amount");

        Map<String, String> valid = new LinkedHashMap<>();
        valid.put("transaction_id", "");
        valid.put("investment_id", "11");
        valid.put("portfolio_id", "10");
        valid.put("portfolio_name", "Core");
        valid.put("symbol", "AAPL");
        valid.put("scheme_code", "");
        valid.put("company_name", "Apple");
        valid.put("asset_type", "STOCK");
        valid.put("transaction_date", "2025-01-10");
        valid.put("transaction_type", "BUY");
        valid.put("quantity", "2");
        valid.put("transaction_price", "120");
        valid.put("transaction_amount", "");

        Map<String, String> invalid = new LinkedHashMap<>(valid);
        invalid.put("investment_id", "999");

        TabularDataService.TabularRow row1 = new TabularDataService.TabularRow(2, valid);
        TabularDataService.TabularRow row2 = new TabularDataService.TabularRow(3, invalid);
        TabularDataService.ParsedTabularData parsed = new TabularDataService.ParsedTabularData(headers, List.of(row1, row2));

        Investment investment = buildInvestment(11, 10, "AAPL", "USD", "STOCK", "5", "100");
        when(tabularDataService.read(file)).thenReturn(parsed);
        when(investmentRepository.findByCustomerId(1)).thenReturn(List.of(investment));
        when(portfolioRepository.findByCustomerId(1)).thenReturn(List.of(new Portfolio(10, 1, "Core")));
        when(investmentRepository.findInvestmentByIdAndCustomerId(11, 1)).thenReturn(Optional.of(investment));

        when(transactionRepository.insertTransaction(any(InvestmentTransaction.class))).thenAnswer(invocation -> {
            InvestmentTransaction tx = invocation.getArgument(0);
            tx.setTransactionId(1002);
            return tx;
        });

        ImportSummaryResponse response = transactionService.importTransactions(1, file);

        assertEquals(1, response.getSuccessfulCount());
        assertEquals(1, response.getFailedCount());
        assertEquals(1, response.getFailures().size());
        verify(transactionRepository).insertTransaction(any(InvestmentTransaction.class));
    }

    private Investment buildInvestment(Integer id,
                                       Integer portfolioId,
                                       String symbol,
                                       String currency,
                                       String assetType,
                                       String quantity,
                                       String purchasePrice) {
        Investment investment = new Investment();
        investment.setInvestmentId(id);
        investment.setPortfolioId(portfolioId);
        investment.setSymbol(symbol);
        investment.setCompanyName("Apple");
        investment.setAssetType(assetType);
        investment.setCurrency(currency);
        investment.setQuantity(new BigDecimal(quantity));
        investment.setPurchasePrice(new BigDecimal(purchasePrice));
        investment.setInvestedAmount(investment.getQuantity().multiply(investment.getPurchasePrice()));
        return investment;
    }
}
