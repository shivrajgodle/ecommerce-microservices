package com.learning.order_service.dto.response;

import java.math.BigDecimal;

import com.learning.order_service.entity.OrderStatus;

import lombok.Getter;

@Getter
public class StatusStat {
    private final OrderStatus status;
    private final Long count;
    private final BigDecimal totalAmount;

    /**
     * This exact constructor signature — types AND order — is what a
     * JPQL "new" constructor expression (Step 3) binds against
     * positionally. Get a type wrong (e.g. 'long' instead of 'Long',
     * or reorder the parameters) and the query fails at STARTUP with
     * a clear error, not silently at runtime — Hibernate validates
     * constructor expressions against the actual class when the
     * repository bean is created.
     */
    public StatusStat(OrderStatus status, Long count, BigDecimal totalAmount) {
        this.status = status;
        this.count = count;
        this.totalAmount = totalAmount;
    }
}