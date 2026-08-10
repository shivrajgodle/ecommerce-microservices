package com.learning.order_service.client.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockDecrementItem {
    @NotNull
    private Long productId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
