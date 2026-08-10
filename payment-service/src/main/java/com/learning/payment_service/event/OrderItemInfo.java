package com.learning.payment_service.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemInfo {
    private Long productId;
    private Integer quantity;
    private java.math.BigDecimal priceAtPurchase;
}
