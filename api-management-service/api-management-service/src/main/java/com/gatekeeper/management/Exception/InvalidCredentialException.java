package com.gatekeeper.management.Exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialException extends RuntimeException {
    public InvalidCredentialException(String message, HttpStatus statusCode) {
        super(message);
    }
}
