package com.learning.cart_service.client;

import com.learning.cart_service.client.dto.ApiResponseWrapper;
import com.learning.cart_service.client.dto.ProductInfo;
import com.learning.cart_service.exception.ProductServiceUnavailableException;
import com.learning.cart_service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductClient {

    private final CatalogServiceClient catalogServiceClient;

    // CircuitBreakerFactory is SPRING CLOUD's vendor-neutral abstraction
    // over circuit breaker implementations — Resilience4j today, but
    // this same calling code would be unchanged if the underlying
    // implementation were ever swapped. This is why we call
    // circuitBreakerFactory.create("catalogService") here rather than
    // using Resilience4j's own @CircuitBreaker annotation directly —
    // a deliberate choice to stay on the framework-agnostic facade,
    // consistent with adding spring-cloud-starter-circuitbreaker-
    // resilience4j (the Spring Cloud wrapper) rather than
    // resilience4j-spring-boot3 (the vendor-specific annotations
    // library) back in File 1's pom.xml.

    private final CircuitBreakerFactory<?,?> circuitBreakerFactory;

    public ProductInfo getProduct(Long productId){
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("catalogService");

        // .run(supplier, fallback) — if the supplier throws ANYTHING,
        // the fallback function runs instead, receiving that exception.
        // This happens regardless of whether that exception counted
        // toward the circuit's failure rate (the ignore-exceptions
        // config from Step 5 only affects failure-rate CALCULATION, not
        // whether the fallback fires — that's why we still need the
        // instanceof check below).
        return circuitBreaker.run(()->{
            ApiResponseWrapper<ProductInfo> response = catalogServiceClient.getProductById(productId);
            return response.getData();
        },
                throwable -> handleFallback(productId,throwable));
    }

    private ProductInfo handleFallback(Long productId, @Nullable Throwable throwable) {
        if(throwable instanceof ResourceNotFoundException notFound){
            // A genuinely nonexistent product — re-throw as-is so
            // GlobalExceptionHandler returns a clean 404, exactly as it
            // would for any other "not found" case in this service.
            throw notFound;
        }

        // Everything else — timeout, connection refused, Catalog
        // Service returning a 5xx, or the circuit currently OPEN and
        // rejecting calls outright — becomes this. The caller (Cart
        // Service's own service layer, next file) decides what
        // "can't verify a product right now" means for THAT operation
        // (e.g. reject an add-to-cart outright vs. degrade gracefully
        // on a cart view — we'll make that call explicitly next file).
        log.error("catalog Service call failed for productId={}:{}",productId,throwable.getMessage());
        throw new ProductServiceUnavailableException("Unable to verify product "+ productId +"right now - please try again shortly");
    }

}
