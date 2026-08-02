package com.beantheredonethat.portfoliomanager.dto;

import com.beantheredonethat.portfoliomanager.entity.Customer;

public class CustomerResponse {

    private Integer customerId;
    private String customerName;
    private String username;
    private String email;
    private String phoneNumber;

    public CustomerResponse(Customer customer) {
        this.customerId = customer.getCustomerId();
        this.customerName = customer.getCustomerName();
        this.username = customer.getUsername();
        this.email = customer.getEmail();
        this.phoneNumber = customer.getPhoneNumber();
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
