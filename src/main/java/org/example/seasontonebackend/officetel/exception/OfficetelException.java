package org.example.seasontonebackend.officetel.exception;

public class OfficetelException extends RuntimeException {

    public OfficetelException(String message) {
        super(message);
    }

    public OfficetelException(String message, Throwable cause) {
        super(message, cause);
    }
}