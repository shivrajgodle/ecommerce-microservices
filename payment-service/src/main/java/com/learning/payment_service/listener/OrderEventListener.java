package com.learning.payment_service.listener;

import com.learning.payment_service.event.OrderCreatedEvent;
import com.learning.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final PaymentService paymentService;

    /**
     * groupId here is redundant with application.yml's
     * spring.kafka.consumer.group-id — Spring Data JPA repositories'
     * "convention over configuration" cousin: this annotation-level
     * value would OVERRIDE the yml one if both were present and
     * different. We're setting it in yml as the single source of
     * truth and omitting it here to avoid two places disagreeing.
     */
    @KafkaListener(topics = "order.created")
    public void onOrderCreated(OrderCreatedEvent event){
        log.info("Received OrderCreatedEvent: orderId={},eventId={}",event.getOrderId(),event.getEventId());
        // Any exception thrown from here propagates up to the error
        // handler configured in Step 10 — we deliberately do NOT
        // try/catch broadly here, so retry/DLT logic in ONE place
        // (the error handler) governs behavior consistently, rather
        // than each listener method inventing its own error handling.
        paymentService.handleOrderCreated(event);
    }
}
