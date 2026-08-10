package com.learning.order_service.client;

import com.learning.order_service.client.dto.ApiResponseWrapper;
import com.learning.order_service.client.dto.CartInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "cart-service", path = "/api/v1/cart")
public interface CartServiceClient {

    // We call Cart Service DIRECTLY (bypassing the gateway, same
    // reasoning as Phase F), which means Cart Service's
    // CurrentUserIdArgumentResolver — which reads X-Auth-User-Id off
    // the request — needs that header supplied explicitly here, since
    // there's no gateway JwtValidationFilter in this call path to add
    // it automatically. We forward it ourselves as a plain Feign header.
    @GetMapping
    ApiResponseWrapper<CartInfo> getCart(@RequestHeader("X-Auth-User-Id") Long userId);

    @DeleteMapping
    ApiResponseWrapper<Void> clearCart(@RequestHeader("X-Auth-User-Id") Long userId);

}
