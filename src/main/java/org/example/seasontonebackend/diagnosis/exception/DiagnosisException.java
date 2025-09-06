package org.example.seasontonebackend.diagnosis.exception;

public class DiagnosisException extends RuntimeException {
    public DiagnosisException(String message) {
        super(message);
    }

    public DiagnosisException(String message, Throwable cause) {
        super(message, cause);
    }
}