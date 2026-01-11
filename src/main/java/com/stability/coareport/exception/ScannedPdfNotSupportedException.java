package com.stability.coareport.exception;

public class ScannedPdfNotSupportedException extends RuntimeException {

    public ScannedPdfNotSupportedException(String message) {
        super(message);
    }

    public ScannedPdfNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }
}
