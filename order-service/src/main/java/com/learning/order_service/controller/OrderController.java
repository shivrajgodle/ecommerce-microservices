package com.learning.order_service.controller;

import com.learning.order_service.client.dto.request.CheckoutRequest;
import com.learning.order_service.client.dto.response.OrderDetailResponse;
import com.learning.order_service.client.dto.response.OrderResponse;
import com.learning.order_service.dto.response.ApiResponse;
import com.learning.order_service.security.CurrentUserId;
import com.learning.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import javax.swing.SortOrder;

import org.hibernate.query.SortDirection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderDetail(
            @CurrentUserId Long userId, @PathVariable Long id) {
        OrderDetailResponse result = orderService.getOrderDetail(userId, id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Order retrieved", result));
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @CurrentUserId Long userId,
            @PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrdersForUser(userId, pageable));
    }

}
