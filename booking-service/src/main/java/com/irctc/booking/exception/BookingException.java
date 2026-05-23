package com.irctc.booking.exception;

public class BookingException extends RuntimeException {
    
    private final String errorCode;

    public BookingException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
