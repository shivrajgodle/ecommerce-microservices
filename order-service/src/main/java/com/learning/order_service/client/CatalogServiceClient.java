package com.learning.order_service.client;

import com.learning.order_service.client.dto.ApiResponseWrapper;
import com.learning.order_service.client.dto.ProductInfo;
import com.learning.order_service.client.dto.request.BulkStockDecrementRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "catalog-service", path = "/api/v1/products")
public interface CatalogServiceClient {

    @GetMapping("/{id}")
    ApiResponseWrapper<ProductInfo> getProductById(@PathVariable("id") Long id);

    @PatchMapping("/decrement-stock-bulk")
    ApiResponseWrapper<Void> decrementStockBulk(@RequestBody BulkStockDecrementRequest request);
}
