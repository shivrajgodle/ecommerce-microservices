package com.learning.order_service.entity;
/**
 * The entire checkout saga, reduced to a state machine on this one enum.
 * PENDING is the only state an order is created in. From there, exactly
 * ONE of two things can happen — driven by the payment.succeeded /
 * payment.failed events we'll consume in File 4 — and neither is
 * reversible through this simple model. Deliberately no "SHIPPED"/
 * "DELIVERED" states here; fulfillment tracking would be its own
 * concern (arguably its own service) — we're keeping this model scoped
 * exactly to what the payment saga needs to express.
 */
public enum OrderStatus {
    PENDING,     // created, awaiting the payment saga's outcome
    CONFIRMED,   // payment.succeeded consumed — order is final and valid
    CANCELLED    // payment.failed consumed, OR stock/price check failed at checkout time
}