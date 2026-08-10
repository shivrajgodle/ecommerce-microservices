package com.learning.order_service.controller;

import com.learning.order_service.client.dto.request.CheckoutRequest;
import com.learning.order_service.client.dto.response.OrderResponse;
import com.learning.order_service.dto.response.ApiResponse;
import com.learning.order_service.security.CurrentUserId;
import com.learning.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(@CurrentUserId Long userId, @Valid @RequestBody CheckoutRequest request){
        OrderResponse result = orderService.checkout(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.CREATED.value(), "Order Created , awaiting payment",result));
    }
}
