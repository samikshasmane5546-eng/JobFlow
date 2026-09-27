package com.example.jobflow.exception;

public class NonRetryableException extends RuntimeException {
    public NonRetryableException(String message) {
        super(message);
    }
}