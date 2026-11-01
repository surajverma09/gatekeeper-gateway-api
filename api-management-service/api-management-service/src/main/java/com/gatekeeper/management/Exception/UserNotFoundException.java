package com.gatekeeper.management.Exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message, HttpStatus statusCode) {
        super(message);
    }
}
