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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PortfolioRepository portfolioRepository;
    private final InvestmentRepository investmentRepository;
    private final InvestmentTransactionRepository transactionRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customerRepository,
                           PortfolioRepository portfolioRepository,
                           InvestmentRepository investmentRepository,
                           InvestmentTransactionRepository transactionRepository,
                           BCryptPasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.portfolioRepository = portfolioRepository;
        this.investmentRepository = investmentRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public CustomerResponse register(RegisterRequest request) {

        if (customerRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException(
                    "Username already exists: " + request.getUsername());
        }

        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Email already exists: " + request.getEmail());
        }

        Customer customer = new Customer();
        customer.setCustomerName(request.getCustomerName());
        customer.setUsername(request.getUsername());
        customer.setEmail(request.getEmail());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        return new CustomerResponse(customerRepository.save(customer));
    }

    public LoginResponse login(LoginRequest request) {

        Customer customer = customerRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        return new LoginResponse(
                customer.getCustomerId(),
                customer.getCustomerName(),
                "Login Successful"
        );
    }

    public Integer resolveCustomerId(Authentication authentication, Integer headerCustomerId) {
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            String username = null;

            if (principal instanceof UserDetails userDetails) {
                username = userDetails.getUsername();
            } else if (principal instanceof String principalName
                    && !"anonymousUser".equalsIgnoreCase(principalName)) {
                username = principalName;
            }

            if (username != null && !username.isBlank()) {
                final String resolvedUsername = username;
                return customerRepository.findByUsername(username)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Customer not found for authenticated username: " + resolvedUsername))
                        .getCustomerId();
            }
        }

        if (headerCustomerId != null) {
            return headerCustomerId;
        }

        throw new IllegalArgumentException(
                "Unable to identify customer for profile request. Please log in and try again.");
    }

    public CustomerResponse getCustomerById(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id: " + id));
        return new CustomerResponse(customer);
    }

    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .map(CustomerResponse::new)
                .collect(Collectors.toList());
    }

    public CustomerResponse updateCustomer(Integer id, UpdateCustomerRequest request) {

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id: " + id));

        if (request.getCustomerName() != null) {
            customer.setCustomerName(request.getCustomerName());
        }

        if (request.getUsername() != null) {
            customerRepository.findByUsername(request.getUsername())
                    .filter(existing -> !existing.getCustomerId().equals(id))
                    .ifPresent(existing -> {
                        throw new DuplicateResourceException(
                                "Username already exists: " + request.getUsername());
                    });
            customer.setUsername(request.getUsername());
        }

        if (request.getEmail() != null) {
            customerRepository.findByEmail(request.getEmail())
                    .filter(existing -> !existing.getCustomerId().equals(id))
                    .ifPresent(existing -> {
                        throw new DuplicateResourceException(
                                "Email already exists: " + request.getEmail());
                    });
            customer.setEmail(request.getEmail());
        }

        if (request.getPhoneNumber() != null) {
            customer.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        return new CustomerResponse(customerRepository.save(customer));
    }

    public CustomerResponse updateProfile(Integer customerId, UpdateCustomerRequest request) {
        return updateCustomer(customerId, request);
    }

    public void changePassword(Integer customerId, ChangePasswordRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id: " + customerId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), customer.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect.");
        }

        customer.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        customerRepository.save(customer);
    }

    public AccountSummaryResponse getAccountSummary(Integer customerId) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id: " + customerId));

        int portfolioCount = portfolioRepository.countByCustomerId(customerId);
        int investmentCount = investmentRepository.countByCustomerId(customerId);
        int transactionCount = transactionRepository.countByCustomerId(customerId);

        return new AccountSummaryResponse(portfolioCount, investmentCount, transactionCount);
    }

    public String exportCustomerDataCsv(Integer customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id: " + customerId));

        List<Portfolio> portfolios = portfolioRepository.findByCustomerId(customerId);
        List<Investment> investments = investmentRepository.findByCustomerId(customerId);
        List<InvestmentTransaction> transactions = transactionRepository.findByCustomerId(customerId);

        List<String> rows = new ArrayList<>();

        rows.add("section,customer_id,customer_name,username,email,phone_number");
        rows.add(String.join(",",
                "profile",
                csv(customer.getCustomerId()),
                csv(customer.getCustomerName()),
                csv(customer.getUsername()),
                csv(customer.getEmail()),
                csv(customer.getPhoneNumber())));
        rows.add("");

        rows.add("section,portfolio_id,customer_id,portfolio_name");
        for (Portfolio portfolio : portfolios) {
            rows.add(String.join(",",
                    "portfolio",
                    csv(portfolio.getPortfolioId()),
                    csv(portfolio.getCustomerId()),
                    csv(portfolio.getPortfolioName())));
        }
        rows.add("");

        rows.add("section,investment_id,portfolio_id,symbol,scheme_code,company_name,exchange,currency,asset_type,custom_asset_type,quantity,invested_amount,purchase_price,current_price,current_value,profit_loss,purchase_date");
        for (Investment investment : investments) {
            rows.add(String.join(",",
                    "investment",
                    csv(investment.getInvestmentId()),
                    csv(investment.getPortfolioId()),
                    csv(investment.getSymbol()),
                    csv(investment.getSchemeCode()),
                    csv(investment.getCompanyName()),
                    csv(investment.getExchange()),
                    csv(investment.getCurrency()),
                    csv(investment.getAssetType()),
                    csv(investment.getCustomAssetType()),
                    csv(investment.getQuantity()),
                    csv(investment.getInvestedAmount()),
                    csv(investment.getPurchasePrice()),
                    csv(investment.getCurrentPrice()),
                    csv(investment.getCurrentValue()),
                    csv(investment.getProfitLoss()),
                    csv(investment.getPurchaseDate())));
        }
        rows.add("");

        rows.add("section,transaction_id,investment_id,symbol,company_name,asset_type,transaction_date,transaction_type,quantity,transaction_price,transaction_amount");
        for (InvestmentTransaction transaction : transactions) {
            rows.add(String.join(",",
                    "transaction",
                    csv(transaction.getTransactionId()),
                    csv(transaction.getInvestmentId()),
                    csv(transaction.getSymbol()),
                    csv(transaction.getCompanyName()),
                    csv(transaction.getAssetType()),
                    csv(transaction.getTransactionDate()),
                    csv(transaction.getTransactionType()),
                    csv(transaction.getQuantity()),
                    csv(transaction.getTransactionPrice()),
                    csv(transaction.getTransactionAmount())));
        }

        return String.join("\n", rows);
    }

    public void deleteCustomerProfile(Integer customerId) {
        deleteCustomer(customerId);
    }

    public void deleteCustomer(Integer id) {
        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Customer not found with id: " + id);
        }
        customerRepository.deleteById(id);
    }

    private String csv(Object value) {
        if (value == null) {
            return "\"\"";
        }

        String raw;
        if (value instanceof LocalDate date) {
            raw = date.toString();
        } else {
            raw = String.valueOf(value);
        }

        String escaped = raw.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}