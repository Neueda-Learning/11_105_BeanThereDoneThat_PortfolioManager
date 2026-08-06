package com.beantheredonethat.portfoliomanager.service;

import com.beantheredonethat.portfoliomanager.dto.CreatePortfolioRequest;
import com.beantheredonethat.portfoliomanager.dto.PortfolioResponse;
import com.beantheredonethat.portfoliomanager.dto.UpdatePortfolioRequest;
import com.beantheredonethat.portfoliomanager.entity.Customer;
import com.beantheredonethat.portfoliomanager.entity.Portfolio;
import com.beantheredonethat.portfoliomanager.exception.CustomerNotFoundException;
import com.beantheredonethat.portfoliomanager.exception.PortfolioNotFoundException;
import com.beantheredonethat.portfoliomanager.repository.CustomerRepository;
import com.beantheredonethat.portfoliomanager.repository.PortfolioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    @Test
    void createPortfolio_success_savesAndReturnsResponse() {
        CreatePortfolioRequest request = new CreatePortfolioRequest("Growth");

        when(customerRepository.findById(1)).thenReturn(Optional.of(new Customer()));
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(invocation -> {
            Portfolio p = invocation.getArgument(0);
            p.setPortfolioId(10);
            return p;
        });

        PortfolioResponse response = portfolioService.createPortfolio(1, request);

        assertEquals(10, response.getPortfolioId());
        assertEquals(1, response.getCustomerId());
        assertEquals("Growth", response.getPortfolioName());
        verify(portfolioRepository).save(any(Portfolio.class));
    }

    @Test
    void createPortfolio_customerNotFound_throwsCustomerNotFoundException() {
        when(customerRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class,
                () -> portfolioService.createPortfolio(1, new CreatePortfolioRequest("Growth")));

        verify(portfolioRepository, never()).save(any(Portfolio.class));
    }

    @Test
    void getPortfolioById_success_returnsOwnedPortfolio() {
        Portfolio portfolio = new Portfolio(10, 1, "Core");
        when(portfolioRepository.findByIdAndCustomerId(10, 1)).thenReturn(Optional.of(portfolio));

        PortfolioResponse response = portfolioService.getPortfolioById(10, 1);

        assertEquals(10, response.getPortfolioId());
        assertEquals("Core", response.getPortfolioName());
    }

    @Test
    void getPortfolioById_notFound_throwsPortfolioNotFoundException() {
        when(portfolioRepository.findByIdAndCustomerId(10, 1)).thenReturn(Optional.empty());

        assertThrows(PortfolioNotFoundException.class,
                () -> portfolioService.getPortfolioById(10, 1));
    }

    @Test
    void getAllPortfolios_success_returnsCustomerScopedList() {
        when(portfolioRepository.findByCustomerId(1))
                .thenReturn(List.of(new Portfolio(10, 1, "Core"), new Portfolio(11, 1, "Growth")));

        List<PortfolioResponse> responses = portfolioService.getAllPortfolios(1);

        assertEquals(2, responses.size());
        assertEquals("Core", responses.get(0).getPortfolioName());
        assertEquals("Growth", responses.get(1).getPortfolioName());
    }

    @Test
    void updatePortfolio_success_updatesName() {
        Portfolio existing = new Portfolio(10, 1, "Old");
        UpdatePortfolioRequest request = new UpdatePortfolioRequest("Updated");

        when(portfolioRepository.findByIdAndCustomerId(10, 1)).thenReturn(Optional.of(existing));
        when(portfolioRepository.save(existing)).thenReturn(existing);

        PortfolioResponse response = portfolioService.updatePortfolio(10, 1, request);

        assertEquals("Updated", response.getPortfolioName());
        verify(portfolioRepository).save(existing);
    }

    @Test
    void deletePortfolio_notOwned_throwsPortfolioNotFoundException() {
        when(portfolioRepository.findByIdAndCustomerId(10, 1)).thenReturn(Optional.empty());

        assertThrows(PortfolioNotFoundException.class,
                () -> portfolioService.deletePortfolio(10, 1));

        verify(portfolioRepository, never()).deleteById(any());
    }

    @Test
    void ensurePortfolioOwnership_success_noException() {
        when(portfolioRepository.findByIdAndCustomerId(10, 1)).thenReturn(Optional.of(new Portfolio(10, 1, "Core")));

        portfolioService.ensurePortfolioOwnership(10, 1);

        verify(portfolioRepository).findByIdAndCustomerId(10, 1);
    }
}
