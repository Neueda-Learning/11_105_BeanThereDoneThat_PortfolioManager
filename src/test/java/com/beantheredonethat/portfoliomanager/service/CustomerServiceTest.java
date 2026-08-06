package com.beantheredonethat.portfoliomanager.service;

import com.beantheredonethat.portfoliomanager.dto.AccountSummaryResponse;
import com.beantheredonethat.portfoliomanager.dto.ChangePasswordRequest;
import com.beantheredonethat.portfoliomanager.dto.CustomerResponse;
import com.beantheredonethat.portfoliomanager.dto.LoginRequest;
import com.beantheredonethat.portfoliomanager.dto.LoginResponse;
import com.beantheredonethat.portfoliomanager.dto.RegisterRequest;
import com.beantheredonethat.portfoliomanager.dto.UpdateCustomerRequest;
import com.beantheredonethat.portfoliomanager.entity.Customer;
import com.beantheredonethat.portfoliomanager.entity.Investment;
import com.beantheredonethat.portfoliomanager.entity.InvestmentTransaction;
import com.beantheredonethat.portfoliomanager.entity.Portfolio;
import com.beantheredonethat.portfoliomanager.exception.DuplicateResourceException;
import com.beantheredonethat.portfoliomanager.exception.InvalidCredentialsException;
import com.beantheredonethat.portfoliomanager.exception.ResourceNotFoundException;
import com.beantheredonethat.portfoliomanager.repository.CustomerRepository;
import com.beantheredonethat.portfoliomanager.repository.InvestmentRepository;
import com.beantheredonethat.portfoliomanager.repository.InvestmentTransactionRepository;
import com.beantheredonethat.portfoliomanager.repository.PortfolioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private InvestmentTransactionRepository transactionRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void register_success_encodesAndSavesCustomer() {
        RegisterRequest request = new RegisterRequest();
        request.setCustomerName("Alice Doe");
        request.setUsername("alice");
        request.setPassword("secret123");
        request.setEmail("alice@example.com");
        request.setPhoneNumber("+1 555 123 4567");

        when(customerRepository.existsByUsername("alice")).thenReturn(false);
        when(customerRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("ENCODED");
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            c.setCustomerId(10);
            return c;
        });

        CustomerResponse response = customerService.register(request);

        assertEquals(10, response.getCustomerId());
        assertEquals("alice", response.getUsername());
        verify(passwordEncoder).encode("secret123");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void register_duplicateUsername_throwsDuplicateResourceException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");

        when(customerRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> customerService.register(request));

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void login_success_returnsLoginResponse() {
        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("secret123");

        Customer customer = buildCustomer(15, "Alice Doe", "alice", "HASH", "alice@example.com", "1234567890");
        when(customerRepository.findByUsername("alice")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("secret123", "HASH")).thenReturn(true);

        LoginResponse response = customerService.login(request);

        assertEquals(15, response.getCustomerId());
        assertEquals("Alice Doe", response.getCustomerName());
        assertEquals("Login Successful", response.getMessage());
    }

    @Test
    void login_invalidPassword_throwsInvalidCredentialsException() {
        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("bad-pass");

        Customer customer = buildCustomer(15, "Alice Doe", "alice", "HASH", "alice@example.com", "1234567890");
        when(customerRepository.findByUsername("alice")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("bad-pass", "HASH")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> customerService.login(request));
    }

    @Test
    void resolveCustomerId_withAuthenticatedUserDetails_returnsCustomerIdFromRepository() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        User principal = new User("alice", "n/a", List.of());

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(customerRepository.findByUsername("alice"))
                .thenReturn(Optional.of(buildCustomer(25, "Alice", "alice", "HASH", "a@a.com", "999")));

        Integer resolved = customerService.resolveCustomerId(authentication, 999);

        assertEquals(25, resolved);
    }

    @Test
    void resolveCustomerId_withHeaderFallback_returnsHeaderCustomerId() {
        Integer resolved = customerService.resolveCustomerId(null, 42);
        assertEquals(42, resolved);
    }

    @Test
    void resolveCustomerId_withoutAuthAndHeader_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> customerService.resolveCustomerId(null, null));
    }

    @Test
    void updateCustomer_usernameConflict_throwsDuplicateResourceException() {
        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setUsername("new-username");

        Customer existing = buildCustomer(5, "Alice", "alice", "HASH", "a@a.com", "999");
        Customer conflicting = buildCustomer(6, "Bob", "new-username", "HASH", "b@b.com", "777");

        when(customerRepository.findById(5)).thenReturn(Optional.of(existing));
        when(customerRepository.findByUsername("new-username")).thenReturn(Optional.of(conflicting));

        assertThrows(DuplicateResourceException.class, () -> customerService.updateCustomer(5, request));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsInvalidCredentialsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrong-current");
        request.setNewPassword("newSecret");

        Customer customer = buildCustomer(10, "Alice", "alice", "HASH", "a@a.com", "999");
        when(customerRepository.findById(10)).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("wrong-current", "HASH")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> customerService.changePassword(10, request));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void getAccountSummary_success_returnsRepositoryCounts() {
        when(customerRepository.findById(3)).thenReturn(Optional.of(buildCustomer(3, "C", "u", "h", "e", "p")));
        when(portfolioRepository.countByCustomerId(3)).thenReturn(2);
        when(investmentRepository.countByCustomerId(3)).thenReturn(4);
        when(transactionRepository.countByCustomerId(3)).thenReturn(7);

        AccountSummaryResponse response = customerService.getAccountSummary(3);

        assertEquals(2, response.getPortfolios());
        assertEquals(4, response.getInvestments());
        assertEquals(7, response.getTransactions());
    }

    @Test
    void exportCustomerDataCsv_success_containsSections() {
        Integer customerId = 9;
        Customer customer = buildCustomer(customerId, "Alice", "alice", "HASH", "alice@example.com", "123");

        Portfolio portfolio = new Portfolio(11, customerId, "Growth");

        Investment investment = new Investment();
        investment.setInvestmentId(21);
        investment.setPortfolioId(11);
        investment.setSymbol("AAPL");
        investment.setCompanyName("Apple");
        investment.setAssetType("STOCK");
        investment.setQuantity(new BigDecimal("2"));
        investment.setInvestedAmount(new BigDecimal("200"));
        investment.setPurchasePrice(new BigDecimal("100"));
        investment.setCurrentPrice(new BigDecimal("120"));
        investment.setCurrentValue(new BigDecimal("240"));
        investment.setProfitLoss(new BigDecimal("40"));
        investment.setPurchaseDate(LocalDate.of(2025, 1, 1));

        InvestmentTransaction tx = new InvestmentTransaction();
        tx.setTransactionId(31);
        tx.setInvestmentId(21);
        tx.setSymbol("AAPL");
        tx.setCompanyName("Apple");
        tx.setAssetType("STOCK");
        tx.setTransactionDate(LocalDate.of(2025, 1, 2));
        tx.setTransactionType("BUY");
        tx.setQuantity(new BigDecimal("2"));
        tx.setTransactionPrice(new BigDecimal("100"));
        tx.setTransactionAmount(new BigDecimal("200"));

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(portfolioRepository.findByCustomerId(customerId)).thenReturn(List.of(portfolio));
        when(investmentRepository.findByCustomerId(customerId)).thenReturn(List.of(investment));
        when(transactionRepository.findByCustomerId(customerId)).thenReturn(List.of(tx));

        String csv = customerService.exportCustomerDataCsv(customerId);

        assertNotNull(csv);
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("section,customer_id"));
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("section,portfolio_id"));
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("section,investment_id"));
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("section,transaction_id"));
    }

    @Test
    void deleteCustomer_notFound_throwsResourceNotFoundException() {
        when(customerRepository.existsById(44)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> customerService.deleteCustomer(44));
        verify(customerRepository, never()).deleteById(any());
    }

    @Test
    void deleteCustomer_success_deletesById() {
        when(customerRepository.existsById(44)).thenReturn(true);

        customerService.deleteCustomer(44);

        verify(customerRepository).deleteById(44);
    }

    private Customer buildCustomer(Integer id, String name, String username, String hash, String email, String phone) {
        Customer c = new Customer();
        c.setCustomerId(id);
        c.setCustomerName(name);
        c.setUsername(username);
        c.setPasswordHash(hash);
        c.setEmail(email);
        c.setPhoneNumber(phone);
        return c;
    }
}
