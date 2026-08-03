package com.beantheredonethat.portfoliomanager.exception;

public class PortfolioNotFoundException extends ResourceNotFoundException {

    public PortfolioNotFoundException(String message) {
        super(message);
    }
}