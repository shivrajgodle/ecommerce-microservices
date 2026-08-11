package com.learning.payment_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka // activates @KafkaListener annotation processing — same "inert until enabled" pattern as every other @Enable* we've used
public class KafkaConfig {

    /**
     * DeadLetterPublishingRecoverer automatically republishes a
     * permanently-failing message to "{original-topic}.DLT" — here,
     * order.created.DLT — using the SAME KafkaTemplate this service
     * already has configured for producing its own outcome events.
     *
     * DefaultErrorHandler wraps that recoverer with a RETRY policy:
     * FixedBackOff(1000, 2) means "wait 1 second, retry, wait 1 second,
     * retry again" — 2 retries AFTER the original attempt, so 3 total
     * attempts — before giving up and routing to the DLT. This is a
     * DIFFERENT retry mechanism from Catalog Service's @Retryable
     * (Phase H) — that one retried a specific, expected, transient
     * exception type (optimistic lock conflicts) with exponential
     * backoff; this one is a blanket safety net for a Kafka LISTENER,
     * retrying almost any processing failure before giving up
     * entirely, because we have no more specific signal to act on here.
     */
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object,Object> kafkaTemplate){
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000, 2));
        // Deliberately do NOT retry business-logic failures we already
        // handle deterministically inside handleOrderCreated (a
        // declined payment isn't a processing ERROR — it's a valid
        // outcome, already handled by processFailure without throwing).
        // What actually reaches this error handler is things like a
        // database connection blip, a genuine bug throwing an
        // unexpected exception, etc. — situations retrying a few times
        // genuinely might resolve.
        return handler;
    }
}
