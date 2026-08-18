package com.learning.catalog_service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RowError {
    private int rowNumber; // 1-based, matching what the person sees in Excel
    private String sku;    // null if the row didn't even have a readable SKU
    private String reason;
}
