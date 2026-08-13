package com.learning.review_service.exception;

/**
 * Distinct from BadCredentialsException (401 — "we don't know who you
 * are") and distinct from a generic access-denied concept borrowed from
 * Spring Security (which this service doesn't even depend on — no
 * Security starter here at all, deliberately, same reasoning as Catalog
 * Service back in Phase E). This is OUR OWN authorization exception,
 * mapped to 403 Forbidden: "we know exactly who you are, and the answer
 * is still no." That distinction — 401 vs 403 — is worth being
 * precise about; they mean genuinely different things.
 */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}