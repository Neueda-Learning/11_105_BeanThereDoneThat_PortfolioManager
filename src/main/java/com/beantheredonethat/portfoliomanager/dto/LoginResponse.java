package com.beantheredonethat.portfoliomanager.dto;

public class LoginResponse {

    private Integer customerId;
    private String customerName;
    private String message;

    public LoginResponse(Integer customerId,
                         String customerName,
                         String message) {

        this.customerId = customerId;
        this.customerName = customerName;
        this.message = message;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getMessage() {
        return message;
    }
}