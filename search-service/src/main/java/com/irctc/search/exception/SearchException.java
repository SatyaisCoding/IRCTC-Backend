package com.irctc.search.exception;

public class SearchException extends RuntimeException {
    
    private final String errorCode;

    public SearchException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
