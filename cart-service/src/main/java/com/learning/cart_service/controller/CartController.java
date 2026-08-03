package com.learning.cart_service.controller;

import com.learning.cart_service.client.dto.ApiResponse;
import com.learning.cart_service.dto.request.AddItemRequest;
import com.learning.cart_service.dto.request.UpdateQuantityRequest;
import com.learning.cart_service.dto.response.CartResponse;
import com.learning.cart_service.security.CurrentUserId;
import com.learning.cart_service.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@CurrentUserId Long userId) {
        CartResponse result = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Cart retrieved", result));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @CurrentUserId Long userId, @Valid @RequestBody AddItemRequest request) {
        CartResponse result = cartService.addItem(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(HttpStatus.CREATED.value(), "Item added to cart", result));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateQuantity(
            @CurrentUserId Long userId, @PathVariable Long itemId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        CartResponse result = cartService.updateItemQuantity(userId, itemId, request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Cart item updated", result));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @CurrentUserId Long userId, @PathVariable Long itemId) {
        CartResponse result = cartService.removeItem(userId, itemId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Item removed from cart", result));
    }
}
