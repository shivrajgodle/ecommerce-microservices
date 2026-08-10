package com.learning.catalog_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkStockDecrementRequest {

    @NotEmpty
    @Valid
    private List<StockDecrementItem> items;
}
