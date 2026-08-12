package com.learning.order_service.listener;


import com.learning.order_service.event.PaymentFailedEvent;
import com.learning.order_service.event.PaymentSucceededEvent;
import com.learning.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OrderService orderService;

    // containerFactory names the SPECIFIC bean from Step 4 this
    // listener uses — this is what lets two @KafkaListener methods in
    // the same service each deserialize into a different concrete
    // type, despite type headers being off globally.
    @KafkaListener(topics = "payment.succeeded", containerFactory = "paymentSucceededListenerFactory")
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        log.info("Received PaymentSucceededEvent for order {}", event.getOrderId());
        orderService.handlePaymentSucceeded(event);
    }

    @KafkaListener(topics = "payment.failed", containerFactory = "paymentFailedListenerFactory")
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent for order {}", event.getOrderId());
        orderService.handlePaymentFailed(event);
    }
}