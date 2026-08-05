package com.beantheredonethat.portfoliomanager.service;

import com.beantheredonethat.portfoliomanager.dto.CreatePortfolioRequest;
import com.beantheredonethat.portfoliomanager.dto.PortfolioResponse;
import com.beantheredonethat.portfoliomanager.dto.UpdatePortfolioRequest;
import com.beantheredonethat.portfoliomanager.entity.Portfolio;
import com.beantheredonethat.portfoliomanager.exception.CustomerNotFoundException;
import com.beantheredonethat.portfoliomanager.exception.PortfolioNotFoundException;
import com.beantheredonethat.portfoliomanager.repository.CustomerRepository;
import com.beantheredonethat.portfoliomanager.repository.PortfolioRepository;
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

    public PortfolioResponse getPortfolioById(Integer id, Integer customerId) {
        Portfolio portfolio = portfolioRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new PortfolioNotFoundException(
                        "Portfolio not found with ID: " + id));
        return toResponse(portfolio);
    }

    public List<PortfolioResponse> getAllPortfolios(Integer customerId) {
        return portfolioRepository.findByCustomerId(customerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PortfolioResponse updatePortfolio(Integer id, Integer customerId, UpdatePortfolioRequest request) {
        Portfolio portfolio = portfolioRepository.findByIdAndCustomerId(id, customerId)
            .orElseThrow(() -> new PortfolioNotFoundException(
                        "Portfolio not found with ID: " + id));

        portfolio.setPortfolioName(request.getPortfolioName());
        Portfolio saved = portfolioRepository.save(portfolio);
        return toResponse(saved);
    }

    public void deletePortfolio(Integer id, Integer customerId) {
        portfolioRepository.findByIdAndCustomerId(id, customerId)
            .orElseThrow(() -> new PortfolioNotFoundException(
                        "Portfolio not found with ID: " + id));

        portfolioRepository.deleteById(id);
    }

    public void ensurePortfolioOwnership(Integer portfolioId, Integer customerId) {
        portfolioRepository.findByIdAndCustomerId(portfolioId, customerId)
                .orElseThrow(() -> new PortfolioNotFoundException(
                        "Portfolio not found with ID: " + portfolioId));
    }

    private PortfolioResponse toResponse(Portfolio portfolio) {
        return new PortfolioResponse(portfolio);
    }
}
