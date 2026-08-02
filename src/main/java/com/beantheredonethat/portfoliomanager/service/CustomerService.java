package com.beantheredonethat.portfoliomanager.service;

import com.beantheredonethat.portfoliomanager.dto.CustomerResponse;
import com.beantheredonethat.portfoliomanager.dto.LoginRequest;
import com.beantheredonethat.portfoliomanager.dto.LoginResponse;
import com.beantheredonethat.portfoliomanager.dto.RegisterRequest;
import com.beantheredonethat.portfoliomanager.dto.UpdateCustomerRequest;
import com.beantheredonethat.portfoliomanager.entity.Customer;
import com.beantheredonethat.portfoliomanager.exception.DuplicateResourceException;
import com.beantheredonethat.portfoliomanager.exception.InvalidCredentialsException;
import com.beantheredonethat.portfoliomanager.exception.ResourceNotFoundException;
import com.beantheredonethat.portfoliomanager.repository.CustomerRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customerRepository,
                           BCryptPasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
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

    public void deleteCustomer(Integer id) {
        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Customer not found with id: " + id);
        }
        customerRepository.deleteById(id);
    }
}