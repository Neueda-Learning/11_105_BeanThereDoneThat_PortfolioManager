package com.beantheredonethat.portfoliomanager.controller;

import com.beantheredonethat.portfoliomanager.dto.CreatePortfolioRequest;
import com.beantheredonethat.portfoliomanager.dto.PortfolioResponse;
import com.beantheredonethat.portfoliomanager.dto.UpdatePortfolioRequest;
import com.beantheredonethat.portfoliomanager.exception.PortfolioNotFoundException;
import com.beantheredonethat.portfoliomanager.service.CustomerService;
import com.beantheredonethat.portfoliomanager.service.PortfolioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PortfolioController.class)
@AutoConfigureMockMvc(addFilters = false)
class PortfolioControllerTest {

    @Autowired
        private WebApplicationContext webApplicationContext;

        private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

    @MockBean
    private PortfolioService portfolioService;

    @MockBean
    private CustomerService customerService;

        @BeforeEach
        void setUpMockMvc() {
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        }

    @Test
    void createPortfolio_success_returnsCreatedJson() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(7))).thenReturn(7);
        when(portfolioService.createPortfolio(eq(7), any(CreatePortfolioRequest.class)))
                .thenReturn(new PortfolioResponse(101, 7, "Growth"));

        mockMvc.perform(post("/api/portfolios")
                        .header("X-Customer-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePortfolioRequest("Growth"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.portfolioId").value(101))
                .andExpect(jsonPath("$.customerId").value(7));

        verify(portfolioService).createPortfolio(eq(7), any(CreatePortfolioRequest.class));
    }

    @Test
    void createPortfolio_validationFailure_returnsBadRequest() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(7))).thenReturn(7);

        mockMvc.perform(post("/api/portfolios")
                        .header("X-Customer-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.portfolioName").exists());
    }

    @Test
    void getAllPortfolios_success_returnsArray() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(7))).thenReturn(7);
        when(portfolioService.getAllPortfolios(7)).thenReturn(List.of(
                new PortfolioResponse(101, 7, "Growth"),
                new PortfolioResponse(102, 7, "Core")
        ));

        mockMvc.perform(get("/api/portfolios").header("X-Customer-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].portfolioId").value(101))
                .andExpect(jsonPath("$[1].portfolioName").value("Core"));

        verify(portfolioService).getAllPortfolios(7);
    }

    @Test
    void getPortfolioById_notFound_returns404() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(7))).thenReturn(7);
        when(portfolioService.getPortfolioById(999, 7)).thenThrow(new PortfolioNotFoundException("Portfolio not found"));

        mockMvc.perform(get("/api/portfolios/999").header("X-Customer-Id", "7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Portfolio not found"));
    }

    @Test
    void updatePortfolio_success_returnsJson() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(7))).thenReturn(7);
        when(portfolioService.updatePortfolio(eq(101), eq(7), any(UpdatePortfolioRequest.class)))
                .thenReturn(new PortfolioResponse(101, 7, "Updated"));

        mockMvc.perform(put("/api/portfolios/101")
                        .header("X-Customer-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePortfolioRequest("Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioName").value("Updated"));

        verify(portfolioService).updatePortfolio(eq(101), eq(7), any(UpdatePortfolioRequest.class));
    }

    @Test
    void deletePortfolio_success_returnsNoContent() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(7))).thenReturn(7);

        mockMvc.perform(delete("/api/portfolios/101").header("X-Customer-Id", "7"))
                .andExpect(status().isNoContent());

        verify(portfolioService).deletePortfolio(101, 7);
    }
}
