package com.beantheredonethat.portfoliomanager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateCustomerRequest {

    private String customerName;

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Size(min = 6, message = "Password must contain at least 6 characters")
    private String password;

    @Email(message = "Enter a valid email")
    private String email;

        @Pattern(
            regexp = "^(?=(?:\\D*\\d){7,15}\\D*$)[+()\\-\\s0-9]+$",
            message = "Phone number must be 7 to 15 digits and may include +, spaces, hyphens, or parentheses")
    private String phoneNumber;

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
