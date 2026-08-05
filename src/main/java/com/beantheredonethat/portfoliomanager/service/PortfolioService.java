package com.beantheredonethat.portfoliomanager.service;

import com.beantheredonethat.portfoliomanager.dto.CreatePortfolioRequest;
import com.beantheredonethat.portfoliomanager.dto.PortfolioResponse;
import com.beantheredonethat.portfoliomanager.dto.UpdatePortfolioRequest;
import com.beantheredonethat.portfoliomanager.entity.Portfolio;
import com.beantheredonethat.portfoliomanager.exception.CustomerNotFoundException;
import com.beantheredonethat.portfoliomanager.exception.PortfolioNotFoundException;
import com.beantheredonethat.portfoliomanager.repository.CustomerRepository;
import com.beantheredonethat.portfoliomanager.repository.PortfolioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final CustomerRepository customerRepository;

    public PortfolioService(PortfolioRepository portfolioRepository, CustomerRepository customerRepository) {
        this.portfolioRepository = portfolioRepository;
        this.customerRepository = customerRepository;
    }

    public PortfolioResponse createPortfolio(Integer customerId, CreatePortfolioRequest request) {
        customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found with ID: " + customerId));

        Portfolio portfolio = new Portfolio(customerId, request.getPortfolioName());
        Portfolio saved = portfolioRepository.save(portfolio);
        return toResponse(saved);
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
                        .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found for authenticated username: " + resolvedUsername))
                        .getCustomerId();
            }
        }

        if (headerCustomerId != null) {
            return headerCustomerId;
        }

        throw new IllegalArgumentException(
                "Unable to identify customer for portfolio creation. Please log in and try again.");
    }

    public PortfolioResponse getPortfolioById(Integer id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new PortfolioNotFoundException(
                        "Portfolio not found with ID: " + id));
        return toResponse(portfolio);
    }

    public List<PortfolioResponse> getAllPortfolios() {
        return portfolioRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<PortfolioResponse> getPortfoliosByCustomer(Integer customerId) {
        customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found with ID: " + customerId));

        return portfolioRepository.findByCustomerId(customerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PortfolioResponse updatePortfolio(Integer id, UpdatePortfolioRequest request) {
        Portfolio portfolio = portfolioRepository.findById(id)
            .orElseThrow(() -> new PortfolioNotFoundException(
                        "Portfolio not found with ID: " + id));

        portfolio.setPortfolioName(request.getPortfolioName());
        Portfolio saved = portfolioRepository.save(portfolio);
        return toResponse(saved);
    }

    public void deletePortfolio(Integer id) {
        portfolioRepository.findById(id)
            .orElseThrow(() -> new PortfolioNotFoundException(
                        "Portfolio not found with ID: " + id));

        portfolioRepository.deleteById(id);
    }

    private PortfolioResponse toResponse(Portfolio portfolio) {
        return new PortfolioResponse(portfolio);
    }
}
