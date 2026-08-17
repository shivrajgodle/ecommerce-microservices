package com.learning.order_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class OrderItemResponse {
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal priceAtPurchase;
    private BigDecimal subtotal;
}