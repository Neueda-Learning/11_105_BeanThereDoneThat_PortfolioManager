package com.beantheredonethat.portfoliomanager.controller;

import com.beantheredonethat.portfoliomanager.dto.CreateInvestmentRequest;
import com.beantheredonethat.portfoliomanager.dto.ExchangeRateResponse;
import com.beantheredonethat.portfoliomanager.dto.ImportFailureResponse;
import com.beantheredonethat.portfoliomanager.dto.ImportSummaryResponse;
import com.beantheredonethat.portfoliomanager.dto.InvestmentResponse;
import com.beantheredonethat.portfoliomanager.dto.UpdateInvestmentRequest;
import com.beantheredonethat.portfoliomanager.entity.Investment;
import com.beantheredonethat.portfoliomanager.exception.InvestmentNotFoundException;
import com.beantheredonethat.portfoliomanager.exception.YahooFinanceException;
import com.beantheredonethat.portfoliomanager.service.CustomerService;
import com.beantheredonethat.portfoliomanager.service.InvestmentService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvestmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class InvestmentControllerTest {

    @Autowired
        private WebApplicationContext webApplicationContext;

        private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockBean
    private InvestmentService investmentService;

    @MockBean
    private CustomerService customerService;

        @BeforeEach
        void setUpMockMvc() {
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        }

    @Test
    void createInvestment_success_returnsCreatedJson() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(1))).thenReturn(1);
        when(investmentService.createInvestment(eq(1), any(CreateInvestmentRequest.class)))
                .thenReturn(new InvestmentResponse(buildInvestment(50, 10)));

        CreateInvestmentRequest request = new CreateInvestmentRequest();
        request.setPortfolioId(10);
        request.setSymbol("AAPL");
        request.setCompanyName("Apple");
        request.setAssetType("STOCK");
        request.setQuantity(new BigDecimal("2"));
        request.setPurchasePrice(new BigDecimal("100"));
        request.setPurchaseDate(LocalDate.of(2025, 1, 1));
        request.setCurrency("USD");

        mockMvc.perform(post("/api/investments")
                        .header("X-Customer-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.investmentId").value(50))
                .andExpect(jsonPath("$.symbol").value("AAPL"));

        verify(investmentService).createInvestment(eq(1), any(CreateInvestmentRequest.class));
    }

    @Test
    void createInvestment_validationFailure_returnsBadRequest() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(1))).thenReturn(1);

        mockMvc.perform(post("/api/investments")
                        .header("X-Customer-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.portfolioId").exists())
                .andExpect(jsonPath("$.quantity").exists());
    }

    @Test
    void getAllInvestments_success_returnsArray() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(2))).thenReturn(2);
        when(investmentService.getAllInvestments(2)).thenReturn(List.of(new InvestmentResponse(buildInvestment(30, 10))));

        mockMvc.perform(get("/api/investments").header("X-Customer-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].investmentId").value(30));

        verify(investmentService).getAllInvestments(2);
    }

    @Test
    void getInvestmentById_notFound_returns404() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(2))).thenReturn(2);
        when(investmentService.getInvestmentById(999, 2)).thenThrow(new InvestmentNotFoundException("Missing investment"));

        mockMvc.perform(get("/api/investments/999").header("X-Customer-Id", "2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Missing investment"));
    }

    @Test
    void getInvestmentsByPortfolio_success_returnsArray() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(2))).thenReturn(2);
        when(investmentService.getInvestmentsByPortfolio(10, 2))
                .thenReturn(List.of(new InvestmentResponse(buildInvestment(31, 10))));

        mockMvc.perform(get("/api/portfolios/10/investments").header("X-Customer-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].portfolioId").value(10));

        verify(investmentService).getInvestmentsByPortfolio(10, 2);
    }

    @Test
    void updateInvestment_success_returnsUpdatedJson() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(2))).thenReturn(2);
        when(investmentService.updateInvestment(eq(30), eq(2), any(UpdateInvestmentRequest.class)))
                .thenReturn(new InvestmentResponse(buildInvestment(30, 10)));

        UpdateInvestmentRequest request = new UpdateInvestmentRequest();
        request.setQuantity(new BigDecimal("4"));
        request.setPurchasePrice(new BigDecimal("90"));

        mockMvc.perform(put("/api/investments/30")
                        .header("X-Customer-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.investmentId").value(30));

        verify(investmentService).updateInvestment(eq(30), eq(2), any(UpdateInvestmentRequest.class));
    }

    @Test
    void deleteInvestment_success_returnsNoContent() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(2))).thenReturn(2);

        mockMvc.perform(delete("/api/investments/30").header("X-Customer-Id", "2"))
                .andExpect(status().isNoContent());

        verify(investmentService).deleteInvestment(30, 2);
    }

    @Test
    void exportInvestments_success_returnsCsvAttachment() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(2))).thenReturn(2);
        when(investmentService.exportInvestmentsCsv(2)).thenReturn("id,symbol\n1,AAPL");

        mockMvc.perform(get("/api/investments/export").header("X-Customer-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"investments-customer-2.csv\""))
                .andExpect(content().contentType("text/csv;charset=UTF-8"));

        verify(investmentService).exportInvestmentsCsv(2);
    }

    @Test
    void importTemplate_success_returnsCsvAttachment() throws Exception {
        when(investmentService.exportInvestmentsTemplateCsv()).thenReturn("investment_id,portfolio_id");

        mockMvc.perform(get("/api/investments/import-template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"investment-import-template.csv\""));

        verify(investmentService).exportInvestmentsTemplateCsv();
    }

    @Test
    void importInvestments_success_returnsSummaryJson() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(2))).thenReturn(2);
        ImportSummaryResponse summary = new ImportSummaryResponse(2, 1, List.of(new ImportFailureResponse(5, "bad row")));
        when(investmentService.importInvestments(eq(2), any())).thenReturn(summary);

        MockMultipartFile file = new MockMultipartFile("file", "investments.csv", "text/csv", "a,b".getBytes());

        mockMvc.perform(multipart("/api/investments/import")
                        .file(file)
                        .header("X-Customer-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successfulCount").value(2))
                .andExpect(jsonPath("$.failedCount").value(1));

        verify(investmentService).importInvestments(eq(2), any());
    }

    @Test
    void getExchangeRate_upstreamError_returnsBadGateway() throws Exception {
        when(investmentService.getExchangeRate("USD", "INR"))
                .thenThrow(new YahooFinanceException("Provider unavailable"));

        mockMvc.perform(get("/api/investments/exchange-rate")
                        .param("from", "USD")
                        .param("to", "INR"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Provider unavailable"));
    }

    @Test
    void getExchangeRate_success_returnsJson() throws Exception {
        when(investmentService.getExchangeRate("USD", "INR"))
                .thenReturn(new ExchangeRateResponse("USD", "INR", new BigDecimal("83.12000000")));

        mockMvc.perform(get("/api/investments/exchange-rate")
                        .param("from", "USD")
                        .param("to", "INR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("USD"))
                .andExpect(jsonPath("$.to").value("INR"));

        verify(investmentService).getExchangeRate("USD", "INR");
    }

    @Test
    void getExchangeRate_invalidInput_returnsBadRequest() throws Exception {
        when(investmentService.getExchangeRate("", "INR"))
                .thenThrow(new IllegalArgumentException("Currency is required"));

        mockMvc.perform(get("/api/investments/exchange-rate")
                        .param("from", "")
                        .param("to", "INR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Currency is required"));
    }

    private Investment buildInvestment(int id, int portfolioId) {
        Investment i = new Investment();
        i.setInvestmentId(id);
        i.setPortfolioId(portfolioId);
        i.setSymbol("AAPL");
        i.setCompanyName("Apple");
        i.setAssetType("STOCK");
        i.setCurrency("USD");
        i.setQuantity(new BigDecimal("2"));
        i.setPurchasePrice(new BigDecimal("100"));
        i.setInvestedAmount(new BigDecimal("200"));
        return i;
    }
}
