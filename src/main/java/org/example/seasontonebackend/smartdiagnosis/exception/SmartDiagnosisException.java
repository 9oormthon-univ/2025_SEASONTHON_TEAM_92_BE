package org.example.seasontonebackend.smartdiagnosis.exception;

public class SmartDiagnosisException extends RuntimeException {

    public SmartDiagnosisException(String message) {
        super(message);
    }

    public SmartDiagnosisException(String message, Throwable cause) {
        super(message, cause);
    }
}