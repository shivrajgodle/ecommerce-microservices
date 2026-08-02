package com.learning.cart_service.client;

import com.learning.cart_service.client.dto.ApiResponseWrapper;
import com.learning.cart_service.client.dto.ProductInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * name = "catalog-service" is resolved via Eureka — Spring Cloud
 * LoadBalancer picks a live instance the exact same way the Gateway's
 * lb:// scheme did in Phase D, just without that scheme prefix (Feign
 * handles service-name resolution natively once the Eureka client +
 * LoadBalancer starters are on the classpath, both already in our pom).
 *
 * IMPORTANT: this call goes DIRECTLY from Cart Service to a Catalog
 * Service instance — it does NOT route back through the API Gateway on
 * port 8080. The gateway's job is being the edge for EXTERNAL client
 * traffic; internal service-to-service calls talk to each other
 * directly via service discovery. Routing internal traffic back through
 * the gateway would add an unnecessary hop and make the gateway a
 * bottleneck for east-west (service-to-service) traffic, not just
 * north-south (client-to-service) traffic, which isn't its job.
 */
@FeignClient(name = "catalog-service", path = "/api/v1/products",configuration = CatalogFeignConfig.class)
public interface CatalogServiceClient {

    @GetMapping("\{id}")
    ApiResponseWrapper<ProductInfo> getProductById(@PathVariable("id") Long id);
}
