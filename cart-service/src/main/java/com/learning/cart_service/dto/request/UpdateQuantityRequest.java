package com.learning.cart_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateQuantityRequest {

    @NotNull
    private Integer quantity; // 0 or below is treated as "remove this item" in the service
}

