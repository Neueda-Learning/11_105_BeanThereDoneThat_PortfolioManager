package com.beantheredonethat.portfoliomanager.controller;

import com.beantheredonethat.portfoliomanager.dto.CreateTransactionRequest;
import com.beantheredonethat.portfoliomanager.dto.ImportFailureResponse;
import com.beantheredonethat.portfoliomanager.dto.ImportSummaryResponse;
import com.beantheredonethat.portfoliomanager.dto.TransactionResponse;
import com.beantheredonethat.portfoliomanager.entity.InvestmentTransaction;
import com.beantheredonethat.portfoliomanager.exception.ResourceNotFoundException;
import com.beantheredonethat.portfoliomanager.service.CustomerService;
import com.beantheredonethat.portfoliomanager.service.InvestmentTransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvestmentTransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class InvestmentTransactionControllerTest {

    @Autowired
        private WebApplicationContext webApplicationContext;

        private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockBean
    private InvestmentTransactionService transactionService;

    @MockBean
    private CustomerService customerService;

        @BeforeEach
        void setUpMockMvc() {
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        }

    @Test
    void createTransaction_success_returnsCreatedJson() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(4))).thenReturn(4);
        when(transactionService.createTransaction(eq(4), any(CreateTransactionRequest.class)))
                .thenReturn(new TransactionResponse(buildTransaction(700, 30, "BUY")));

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setInvestmentId(30);
        request.setTransactionType("BUY");
        request.setQuantity(new BigDecimal("1"));
        request.setTransactionPrice(new BigDecimal("100"));
        request.setTransactionDate(LocalDate.of(2025, 1, 10));

        mockMvc.perform(post("/api/transactions")
                        .header("X-Customer-Id", "4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value(700))
                .andExpect(jsonPath("$.transactionType").value("BUY"));

        verify(transactionService).createTransaction(eq(4), any(CreateTransactionRequest.class));
    }

    @Test
    void createTransaction_validationFailure_returnsBadRequest() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(4))).thenReturn(4);

        mockMvc.perform(post("/api/transactions")
                        .header("X-Customer-Id", "4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.investmentId").exists())
                .andExpect(jsonPath("$.transactionType").exists());
    }

    @Test
    void getAllTransactions_success_returnsArray() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(4))).thenReturn(4);
        when(transactionService.getAllTransactions(4))
                .thenReturn(List.of(new TransactionResponse(buildTransaction(701, 30, "BUY"))));

        mockMvc.perform(get("/api/transactions").header("X-Customer-Id", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionId").value(701));

        verify(transactionService).getAllTransactions(4);
    }

    @Test
    void getTransactionById_notFound_returns404() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(4))).thenReturn(4);
                when(transactionService.getTransactionById(999, 4)).thenThrow(new ResourceNotFoundException("Transaction missing"));

        mockMvc.perform(get("/api/transactions/999").header("X-Customer-Id", "4"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Transaction missing"));
    }

    @Test
    void getTransactionsByInvestment_success_returnsArray() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(4))).thenReturn(4);
        when(transactionService.getTransactionsByInvestment(30, 4))
                .thenReturn(List.of(new TransactionResponse(buildTransaction(702, 30, "SELL"))));

        mockMvc.perform(get("/api/investments/30/transactions").header("X-Customer-Id", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionType").value("SELL"));

        verify(transactionService).getTransactionsByInvestment(30, 4);
    }

    @Test
    void deleteTransaction_success_returnsNoContent() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(4))).thenReturn(4);

        mockMvc.perform(delete("/api/transactions/700").header("X-Customer-Id", "4"))
                .andExpect(status().isNoContent());

        verify(transactionService).deleteTransaction(700, 4);
    }

    @Test
    void exportTransactions_success_returnsCsvAttachment() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(4))).thenReturn(4);
        when(transactionService.exportTransactionsCsv(4)).thenReturn("id,amount\n1,100");

        mockMvc.perform(get("/api/transactions/export").header("X-Customer-Id", "4"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"transactions-customer-4.csv\""))
                .andExpect(content().contentType("text/csv;charset=UTF-8"));

        verify(transactionService).exportTransactionsCsv(4);
    }

    @Test
    void downloadTransactionTemplate_success_returnsCsvAttachment() throws Exception {
        when(transactionService.exportTransactionsTemplateCsv()).thenReturn("transaction_id,investment_id");

        mockMvc.perform(get("/api/transactions/import-template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"transaction-import-template.csv\""));

        verify(transactionService).exportTransactionsTemplateCsv();
    }

    @Test
    void importTransactions_success_returnsSummaryJson() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(4))).thenReturn(4);
        when(transactionService.importTransactions(eq(4), any())).thenReturn(
                new ImportSummaryResponse(3, 1, List.of(new ImportFailureResponse(4, "bad data")))
        );

        MockMultipartFile file = new MockMultipartFile("file", "tx.csv", "text/csv", "a,b".getBytes());

        mockMvc.perform(multipart("/api/transactions/import")
                        .file(file)
                        .header("X-Customer-Id", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successfulCount").value(3))
                .andExpect(jsonPath("$.failedCount").value(1));

        verify(transactionService).importTransactions(eq(4), any());
    }

    private InvestmentTransaction buildTransaction(int id, int investmentId, String type) {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.setTransactionId(id);
        tx.setInvestmentId(investmentId);
        tx.setSymbol("AAPL");
        tx.setCompanyName("Apple");
        tx.setAssetType("STOCK");
        tx.setCurrency("USD");
        tx.setTransactionDate(LocalDate.of(2025, 1, 10));
        tx.setTransactionType(type);
        tx.setQuantity(new BigDecimal("1"));
        tx.setTransactionPrice(new BigDecimal("100"));
        tx.setTransactionAmount(new BigDecimal("100"));
        return tx;
    }
}
