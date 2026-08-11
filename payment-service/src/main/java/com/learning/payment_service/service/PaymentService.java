package com.learning.payment_service.service;

import com.learning.payment_service.Repository.PaymentRepository;
import com.learning.payment_service.Repository.ProcessedEventRepository;
import com.learning.payment_service.entity.Payment;
import com.learning.payment_service.entity.PaymentStatus;
import com.learning.payment_service.entity.ProcessedEvent;
import com.learning.payment_service.event.OrderCreatedEvent;
import com.learning.payment_service.event.PaymentFailedEvent;
import com.learning.payment_service.event.PaymentSucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String,Object> kafkaTemplate;

    // No real payment gateway integration in this learning project —
    // this threshold stands in for "the card was declined" so both
    // outcomes are easy to trigger and test deliberately (place a
    // normal order vs. an intentionally large one).
    private static final BigDecimal DECLINE_THRESHOLD = new BigDecimal("5000.00");

    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event){
        // THE IDEMPOTENCY CHECK — the entire reason ProcessedEvent
        // exists. This MUST be the first thing that happens, before any
        // other side effect (saving a Payment, publishing an outcome
        // event) — checking it late would defeat the purpose entirely.
        if(processedEventRepository.existsByEventId(event.getEventId())){
            log.info("Event {} already processed — skipping (likely Kafka redelivery)", event.getEventId());
            return;
        }

        log.info("Processing payment for order {}, amount {}", event.getOrderId(), event.getTotalAmount());

        boolean approved = event.getTotalAmount().compareTo(DECLINE_THRESHOLD) < 0;

        if(approved){
            processSuccess(event);
        }else{
            processFailure(event,"Amount exceeds simulated authorization limit");
        }

        // Marking the event processed is the LAST step, inside the SAME
        // transaction as everything above. If ANYTHING earlier in this
        // method throws, the whole transaction rolls back — including
        // this insert — meaning the event correctly remains
        // "unprocessed" and WILL be retried, exactly as it should be.
        processedEventRepository.save(new ProcessedEvent(event.getEventId()));
    }

    private void processSuccess(OrderCreatedEvent event) {
        Payment payment = new Payment(event.getOrderId(), event.getUserId(),event.getTotalAmount(), PaymentStatus.SUCCEEDED,null);

        Payment saved = paymentRepository.save(payment);

        PaymentSucceededEvent outcome = new PaymentSucceededEvent(UUID.randomUUID().toString(),
                event.getOrderId(),
                saved.getId(),
                event.getTotalAmount(),
                Instant.now());

        kafkaTemplate.send("payment.succeeded",event.getOrderId().toString(),outcome);
        log.info("payment succeeded for order {}", event.getOrderId());
    }

    private void processFailure(OrderCreatedEvent event,String reason) {
        Payment payment = new Payment(event.getOrderId(), event.getUserId(), event.getTotalAmount(),PaymentStatus.FAILED,reason);
        paymentRepository.save(payment);

        PaymentFailedEvent outcome = new PaymentFailedEvent(UUID.randomUUID().toString(), event.getOrderId(), reason, Instant.now());
        kafkaTemplate.send("payment.failed",event.getOrderId().toString(),outcome);
        log.info("Payment failed for order {}:{}",event.getOrderId(),reason);
    }
}
