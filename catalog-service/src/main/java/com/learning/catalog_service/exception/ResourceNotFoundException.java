package com.learning.catalog_service.exception;

/**
 * Distinct from DuplicateResourceException (already written in Phase C
 * File 3c) — separating "the thing you asked for doesn't exist" (404)
 * from "the thing you're creating already exists" (409) at the TYPE
 * level, not just the message, is what lets the handler below map each
 * to the correct HTTP status automatically rather than guessing from
 * string content.
 */
public class ResourceNotFoundException extends RuntimeException{
    public ResourceNotFoundException(String message){
        super(message);
    }
}
