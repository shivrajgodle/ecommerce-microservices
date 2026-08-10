package com.learning.order_service.client.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductInfo {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;
    private boolean active;
}
