package com.learning.order_service.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreatedEvent {
    private String eventId;
    private Long orderId;
    private Long userId;
    private BigDecimal totalAmount;
    private List<OrderItemInfo> items;
    private Instant occurredAt;
}
