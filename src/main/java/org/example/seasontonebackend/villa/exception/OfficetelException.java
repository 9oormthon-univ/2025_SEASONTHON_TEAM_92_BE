package org.example.seasontonebackend.villa.exception;

public class OfficetelException extends RuntimeException {

    public OfficetelException(String message) {
        super(message);
    }

    public OfficetelException(String message, Throwable cause) {
        super(message, cause);
    }
}