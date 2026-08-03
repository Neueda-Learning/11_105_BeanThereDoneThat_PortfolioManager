package com.beantheredonethat.portfoliomanager.repository;

import com.beantheredonethat.portfoliomanager.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioRepository extends JpaRepository<Portfolio, Integer> {

    List<Portfolio> findByCustomerCustomerId(Integer customerId);
}
