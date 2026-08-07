package com.learning.order_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_order_user", columnList = "user_id"),
                @Index(name = "idx_order_status", columnList = "status")
        }
)
public class Order extends BaseEntity {

    // Cross-service reference again — same reasoning as Cart.userId,
    // no FK possible or attempted.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * @Embedded pairs with the class's own @Embeddable — Hibernate
     * flattens ShippingAddress's fields directly as columns on THIS
     * table (orders.shipping_street, orders.shipping_city, etc.), no
     * join, no separate table. One Java object, zero extra SQL cost
     * over just declaring five String fields directly here — the
     * benefit is purely code organization (grouping related fields,
     * reusing the ShippingAddress shape if another entity ever needed
     * an embedded address too).
     */
    @Embedded
    private ShippingAddress shippingAddress;

    // Populated once payment.failed is consumed (File 4) — null for
    // any order that's still PENDING or successfully CONFIRMED.
    @Column(name = "cancellation_reason",length = 500)
    private String cancellationReason;

    /**
     * Same reasoning as Product.version back in Catalog Service — this
     * order's status can be modified from more than one place
     * (potentially a future admin cancellation endpoint, AND the Kafka
     * listener reacting to payment outcome). @Version protects against
     * a lost update if those ever raced.
     */
    @Version
    private Long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();


    public Order(Long userId, ShippingAddress shippingAddress, BigDecimal totalAmount){
        this.userId = userId;
        this.shippingAddress = shippingAddress;
        this.totalAmount = totalAmount;
    }

    public void addItem(OrderItem orderItem){
        items.add(orderItem);
        items.setOrder(this);
    }

    /**
     * STATE MACHINE GUARDS. These aren't just setters — they enforce
     * the invariant that a transition is only valid from PENDING. This
     * matters concretely once File 4's Kafka listener exists: Kafka's
     * at-least-once delivery (Phase G) means a payment.succeeded event
     * COULD be delivered twice. Calling confirm() a second time on an
     * already-CONFIRMED order throws instead of silently re-processing
     * — this is the entity itself enforcing idempotency at the domain
     * level, as one layer of the broader idempotent-consumer strategy
     * flagged back in Phase G (the eventId dedup check is the other,
     * complementary layer — we'll use both together in File 4).
     */
    public void confirm(){
        if(this.status != OrderStatus.PENDING){
            throw new IllegalStateException("Cannot confirm order "+getId() +"- current status is "+this.status);
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void cancel(String reason){
        if(this.status != OrderStatus.PENDING){
            throw new IllegalStateException("Cannot cancel order"+getId()+"- current status is"+this.status);
        }
        this.status = OrderStatus.CANCELLED;
        this.cancellationReason = reason;
    }

}


















