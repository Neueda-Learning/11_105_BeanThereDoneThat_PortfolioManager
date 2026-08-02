package com.beantheredonethat.portfoliomanager.service;

import com.beantheredonethat.portfoliomanager.dto.LoginRequest;
import com.beantheredonethat.portfoliomanager.dto.LoginResponse;
import com.beantheredonethat.portfoliomanager.dto.RegisterRequest;
import com.beantheredonethat.portfoliomanager.entity.Customer;
import com.beantheredonethat.portfoliomanager.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public String register(RegisterRequest request) {

        if (customerRepository.existsByUsername(request.getUsername())) {
            return "Username already exists";
        }

        if (customerRepository.existsByEmail(request.getEmail())) {
            return "Email already exists";
        }

        Customer customer = new Customer();

        customer.setCustomerName(request.getCustomerName());
        customer.setUsername(request.getUsername());
        customer.setEmail(request.getEmail());
        customer.setPhoneNumber(request.getPhoneNumber());

        customer.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        customerRepository.save(customer);

        return "Customer registered successfully";
    }

    public LoginResponse login(LoginRequest request) {

        Customer customer = customerRepository
                .findByUsername(request.getUsername())
                .orElse(null);

        if (customer == null) {
            return new LoginResponse(
                    null,
                    null,
                    "Invalid username"
            );
        }

        boolean matched = passwordEncoder.matches(
                request.getPassword(),
                customer.getPasswordHash()
        );

        if (!matched) {
            return new LoginResponse(
                    null,
                    null,
                    "Invalid password"
            );
        }

        return new LoginResponse(
                customer.getCustomerId(),
                customer.getCustomerName(),
                "Login Successful"
        );
    }
}