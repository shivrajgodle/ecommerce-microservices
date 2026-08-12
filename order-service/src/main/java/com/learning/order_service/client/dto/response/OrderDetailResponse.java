package com.learning.order_service.client.dto.response;


import com.learning.order_service.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderDetailResponse {
    private Long id;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdDate;
    private String cancellationReason; // null unless status == CANCELLED
    private ShippingAddressResponse shippingAddress;
    private List<OrderItemResponse> items;
}