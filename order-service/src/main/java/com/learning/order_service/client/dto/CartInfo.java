package com.learning.order_service.client.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CartInfo {
    private Long id;
    private Long userId;
    private List<CartItemInfo> items;
}
