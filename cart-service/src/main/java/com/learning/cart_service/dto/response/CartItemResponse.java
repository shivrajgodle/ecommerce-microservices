package com.learning.cart_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String productName; // "Unavailable" if Catalog Service couldn't be reached — see toItemResponse below
    private Integer quantity;
    private BigDecimal priceSnapshot;
    private BigDecimal subtotal;
}
