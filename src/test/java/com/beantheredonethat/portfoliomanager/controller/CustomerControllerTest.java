package com.beantheredonethat.portfoliomanager.controller;

import com.beantheredonethat.portfoliomanager.dto.AccountSummaryResponse;
import com.beantheredonethat.portfoliomanager.dto.ChangePasswordRequest;
import com.beantheredonethat.portfoliomanager.dto.CustomerResponse;
import com.beantheredonethat.portfoliomanager.dto.LoginResponse;
import com.beantheredonethat.portfoliomanager.dto.PortfolioResponse;
import com.beantheredonethat.portfoliomanager.entity.Customer;
import com.beantheredonethat.portfoliomanager.exception.DuplicateResourceException;
import com.beantheredonethat.portfoliomanager.exception.InvalidCredentialsException;
import com.beantheredonethat.portfoliomanager.exception.ResourceNotFoundException;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
class CustomerControllerTest {

    @Autowired
        private WebApplicationContext webApplicationContext;

        private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockBean
    private CustomerService customerService;

    @MockBean
    private PortfolioService portfolioService;

        @BeforeEach
        void setUpMockMvc() {
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        }

    @Test
    void register_success_returnsCreatedWithJson() throws Exception {
        CustomerResponse response = customerResponse(1, "Alice", "alice", "alice@example.com", "1234567890");
        when(customerService.register(any())).thenReturn(response);

        Map<String, Object> body = Map.of(
                "customerName", "Alice",
                "username", "alice",
                "password", "secret123",
                "email", "alice@example.com",
                "phoneNumber", "1234567890"
        );

        mockMvc.perform(post("/api/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.username").value("alice"));

        verify(customerService).register(any());
    }

    @Test
    void register_validationFailure_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.customerName").exists())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.password").exists());

        verify(customerService, never()).register(any());
    }

    @Test
    void register_duplicateUsername_returnsConflictFromGlobalHandler() throws Exception {
        when(customerService.register(any())).thenThrow(new DuplicateResourceException("Username already exists"));

        Map<String, Object> body = Map.of(
                "customerName", "Alice",
                "username", "alice",
                "password", "secret123",
                "email", "alice@example.com",
                "phoneNumber", "1234567890"
        );

        mockMvc.perform(post("/api/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username already exists"));
    }

    @Test
    void login_success_returnsOkWithJson() throws Exception {
        when(customerService.login(any())).thenReturn(new LoginResponse(10, "Alice", "Login Successful"));

        mockMvc.perform(post("/api/customers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(10))
                .andExpect(jsonPath("$.customerName").value("Alice"));

        verify(customerService).login(any());
        }

        @Test
        void login_invalidCredentials_returnsUnauthorized() throws Exception {
        when(customerService.login(any())).thenThrow(new InvalidCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/api/customers/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"bad\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Invalid username or password"));

        verify(customerService).login(any());
    }

    @Test
    void getCustomerById_success_returnsCustomerJson() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(10))).thenReturn(10);
        when(customerService.getCustomerById(10)).thenReturn(customerResponse(10, "Alice", "alice", "alice@example.com", "123"));

        mockMvc.perform(get("/api/customers/10").header("X-Customer-Id", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(10))
                .andExpect(jsonPath("$.customerName").value("Alice"));

        verify(customerService).resolveCustomerId(any(), eq(10));
        verify(customerService).getCustomerById(10);
    }

    @Test
    void getCustomerById_mismatch_returnsForbidden() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(10))).thenReturn(10);

        mockMvc.perform(get("/api/customers/99").header("X-Customer-Id", "10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied for requested customer ID."));
    }

    @Test
    void getProfileSummary_success_returnsCounts() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(7))).thenReturn(7);
        when(customerService.getAccountSummary(7)).thenReturn(new AccountSummaryResponse(2, 5, 9));

        mockMvc.perform(get("/api/customers/profile/summary").header("X-Customer-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolios").value(2))
                .andExpect(jsonPath("$.investments").value(5))
                .andExpect(jsonPath("$.transactions").value(9));

        verify(customerService).getAccountSummary(7);
        }

        @Test
        void getCurrentProfile_success_returnsCustomerJson() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(12))).thenReturn(12);
        when(customerService.getCustomerById(12)).thenReturn(customerResponse(12, "Sam", "sam", "sam@example.com", "555"));

        mockMvc.perform(get("/api/customers/profile").header("X-Customer-Id", "12"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customerId").value(12))
            .andExpect(jsonPath("$.username").value("sam"));

        verify(customerService).resolveCustomerId(any(), eq(12));
        verify(customerService).getCustomerById(12);
    }

    @Test
    void exportProfileData_success_returnsCsvAttachment() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(7))).thenReturn(7);
        when(customerService.exportCustomerDataCsv(7)).thenReturn("section,customer_id\nprofile,7");

        mockMvc.perform(get("/api/customers/profile/export").header("X-Customer-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"portfolio-data-customer-7.csv\""))
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string("section,customer_id\nprofile,7"));
    }

    @Test
    void updateProfile_success_returnsUpdatedCustomer() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(8))).thenReturn(8);
        when(customerService.updateProfile(eq(8), any()))
                .thenReturn(customerResponse(8, "Alice Updated", "alice", "alice@example.com", "123"));

        mockMvc.perform(put("/api/customers/profile")
                        .header("X-Customer-Id", "8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"Alice Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(8))
                .andExpect(jsonPath("$.customerName").value("Alice Updated"));

        verify(customerService).updateProfile(eq(8), any());
        }

        @Test
        void changePassword_success_returnsNoContent() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(5))).thenReturn(5);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old-pass");
        request.setNewPassword("new-pass-123");

        mockMvc.perform(put("/api/customers/profile/password")
                .header("X-Customer-Id", "5")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        verify(customerService).changePassword(eq(5), any());
    }

    @Test
    void changePassword_validationFailure_returnsBadRequest() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(5))).thenReturn(5);

        mockMvc.perform(put("/api/customers/profile/password")
                        .header("X-Customer-Id", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.currentPassword").exists())
                .andExpect(jsonPath("$.newPassword").exists());
    }

    @Test
    void deleteProfile_success_returnsNoContent() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(5))).thenReturn(5);

        mockMvc.perform(delete("/api/customers/profile").header("X-Customer-Id", "5"))
                .andExpect(status().isNoContent());

        verify(customerService).deleteCustomerProfile(5);
    }

    @Test
    void getAllCustomers_success_returnsSingleItemList() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(5))).thenReturn(5);
        when(customerService.getCustomerById(5)).thenReturn(customerResponse(5, "Alice", "alice", "alice@example.com", "123"));

        mockMvc.perform(get("/api/customers").header("X-Customer-Id", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value(5));

        verify(customerService).getCustomerById(5);
    }

    @Test
    void updateCustomer_notFound_returns404() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(11))).thenReturn(11);
        when(customerService.updateCustomer(eq(11), any())).thenThrow(new ResourceNotFoundException("Customer missing"));

        mockMvc.perform(put("/api/customers/11")
                        .header("X-Customer-Id", "11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"X\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Customer missing"));
    }

    @Test
    void deleteCustomer_success_returnsNoContent() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(11))).thenReturn(11);

        mockMvc.perform(delete("/api/customers/11").header("X-Customer-Id", "11"))
                .andExpect(status().isNoContent());

        verify(customerService).deleteCustomer(11);
    }

    @Test
    void getPortfoliosByCustomer_success_returnsPortfolioArray() throws Exception {
        when(customerService.resolveCustomerId(any(), eq(3))).thenReturn(3);
        when(portfolioService.getAllPortfolios(3)).thenReturn(List.of(new PortfolioResponse(55, 3, "Core")));

        mockMvc.perform(get("/api/customers/3/portfolios").header("X-Customer-Id", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].portfolioId").value(55))
                .andExpect(jsonPath("$[0].portfolioName").value("Core"));

        verify(portfolioService).getAllPortfolios(3);
    }

    private CustomerResponse customerResponse(int id, String name, String username, String email, String phone) {
        Customer c = new Customer();
        c.setCustomerId(id);
        c.setCustomerName(name);
        c.setUsername(username);
        c.setEmail(email);
        c.setPhoneNumber(phone);
        return new CustomerResponse(c);
    }
}
