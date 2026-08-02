package com.beantheredonethat.portfoliomanager.repository;

import com.beantheredonethat.portfoliomanager.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByUsername(String username);

    Optional<Customer> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}