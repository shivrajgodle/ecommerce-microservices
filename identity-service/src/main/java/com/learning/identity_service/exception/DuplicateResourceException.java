package com.learning.identity_service.exception;

/**
 * Deliberately minimal for now — no @ControllerAdvice wired up yet, so
 * this will currently surface as Spring Boot's generic default error
 * JSON (still valid JSON, just not our standardized shape). We're
 * building a proper global exception handler as its own dedicated file
 * right after this auth flow — flagging that explicitly so the current
 * behavior doesn't look like an oversight.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message){
        super(message);
    }
}
