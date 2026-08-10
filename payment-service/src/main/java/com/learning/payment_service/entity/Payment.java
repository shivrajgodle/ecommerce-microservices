package com.learning.payment_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "payments",
        // Second, independent layer of idempotency protection, at the
        // DATABASE level this time — even if the eventId dedup check
        // (Step 8) somehow got bypassed, this constraint makes it
        // physically impossible to insert two Payment rows for the same
        // order. Two layers protecting the same invariant, from different
        // angles — worth noticing this same "defense in depth" pattern is
        // also what Order.confirm()'s state guard (Phase H) provides on
        // the OTHER side of this saga.
        uniqueConstraints = @UniqueConstraint(columnNames = "order_id")
)
@Getter
@Setter
@NoArgsConstructor
public class Payment extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    public Payment(Long orderId, Long userId, BigDecimal amount, PaymentStatus status, String failureReason) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.failureReason = failureReason;
    }
}