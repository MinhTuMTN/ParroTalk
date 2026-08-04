package com.parrotalk.backend.exception;

public class EmailTemplateException extends EmailException {
    public EmailTemplateException(String message) {
        super(message);
    }
    
    public EmailTemplateException(String message, Throwable cause) {
        super(message, cause);
    }
}
