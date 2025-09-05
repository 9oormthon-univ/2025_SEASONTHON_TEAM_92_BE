package org.example.seasontonebackend.mission.exception;

public class MissionException extends RuntimeException {
    public MissionException(String message) {
        super(message);
    }

    public MissionException(String message, Throwable cause) {
        super(message, cause);
    }
}