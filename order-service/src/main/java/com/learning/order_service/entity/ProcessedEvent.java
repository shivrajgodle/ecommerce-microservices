package com.learning.order_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * THE idempotent-consumer mechanism flagged all the way back in Phase G.
 * Before acting on ANY incoming event, we check whether its eventId is
 * already in this table. If it is, we've seen this exact event before
 * (Kafka redelivered it — a rebalance, a consumer restart after
 * processing-but-before-committing, etc.) and we skip processing
 * entirely rather than risk creating a duplicate Payment or publishing
 * a duplicate outcome event.
 *
 * A separate table from Payment itself (rather than, say, just checking
 * "does a Payment already exist for this orderId") is deliberate: it
 * lets us dedupe by the EVENT's identity, which is correct even for
 * event types that don't map 1:1 to a single entity — a pattern that
 * generalizes if this service ever needs to consume additional event
 * types beyond order.created.
 */
@Entity
@Table(name = "processed_events")
@Getter
@Setter
@NoArgsConstructor
public class ProcessedEvent extends BaseEntity{

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    public ProcessedEvent(String eventId) {
        this.eventId = eventId;
    }
}
