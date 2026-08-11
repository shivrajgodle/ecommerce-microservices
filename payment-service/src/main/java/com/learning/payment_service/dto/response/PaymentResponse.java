package com.learning.payment_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.learning.payment_service.entity.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Builder
@Getter
@Setter
public class PaymentResponse {

    private Long id;

    private Long orderId;

    private BigDecimal amount;

    private PaymentStatus status; // or PaymentStatus if you're using an enum

    private String failureReason; // null when payment succeeded

}