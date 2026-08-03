package com.beantheredonethat.portfoliomanager.exception;

public class CustomerNotFoundException extends ResourceNotFoundException {

    public CustomerNotFoundException(String message) {
        super(message);
    }
}