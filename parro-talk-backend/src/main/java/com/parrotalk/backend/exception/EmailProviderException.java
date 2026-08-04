package com.parrotalk.backend.exception;

public class EmailProviderException extends EmailException {
    public EmailProviderException(String message) {
        super(message);
    }
    
    public EmailProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
