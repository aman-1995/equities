package com.equities.service;

public class TransactionEditException extends RuntimeException {
    
    public TransactionEditException(String message) {
        super(message);
    }
    
    public TransactionEditException(String message, Throwable cause) {
        super(message, cause);
    }
}
