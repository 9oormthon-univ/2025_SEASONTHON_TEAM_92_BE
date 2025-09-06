package org.example.seasontonebackend.Mission.exception;

public class MissionException extends RuntimeException {

    public MissionException(String message) {
        super(message);
    }

    public MissionException(String message, Throwable cause) {
        super(message, cause);
    }
}