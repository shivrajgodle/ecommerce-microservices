package com.learning.catalog_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

@Getter
@Setter
public class ProductRequest {

    @NotBlank
    @Size(max = 50)
    private String sku;

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotNull
    @DecimalMin(value = "0.0",inclusive = false,message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull
    @Min(value = 0,message = "Stock cannot be negative")
    private Integer stockQuantity;

    @NotNull(message = "categoryId is required")
    private Long categoryId;

    private Set<String> tagNames; // optional — tags created if they don't already exist
}
