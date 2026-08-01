package com.learning.catalog_service.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductSearchRequest {
    private Long categoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String keyword;
    private List<String> tags;
}
