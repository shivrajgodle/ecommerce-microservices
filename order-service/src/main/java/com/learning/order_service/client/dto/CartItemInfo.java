package com.learning.order_service.client.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemInfo {
    private Long productId;
    private Integer quantity;
}
