package com.learning.order_service.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.learning.order_service.entity.OrderStatus;

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