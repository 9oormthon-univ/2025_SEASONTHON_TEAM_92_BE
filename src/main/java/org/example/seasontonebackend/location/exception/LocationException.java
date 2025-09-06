package org.example.seasontonebackend.location.exception;

/**
 * 위치 인증 관련 예외 클래스
 */
public class LocationException extends RuntimeException {

    public LocationException(String message) {
        super(message);
    }

    public LocationException(String message, Throwable cause) {
        super(message, cause);
    }
}