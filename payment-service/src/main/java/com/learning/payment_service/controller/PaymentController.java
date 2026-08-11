package com.learning.payment_service.controller;

import com.learning.payment_service.Repository.PaymentRepository;
import com.learning.payment_service.dto.response.ApiResponse;
import com.learning.payment_service.dto.response.PaymentResponse;
import com.learning.payment_service.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getOrderById(@PathVariable Long orderId){
        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow(()-> new ResourceNotFoundException("No payment found for order " + orderId));

        PaymentResponse response = PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .failureReason(payment.getFailureReason())
                .build();

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Payment retrieved",response));
    }

}
