package com.learning.order_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;


    // Cross-service reference to Catalog Service's Product — same
    // pattern as CartItem.productId.
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * UNLIKE CartItem, we DO denormalize the product name here, not
     * just the price. Reasoning: an Order, once placed, is a permanent
     * historical record — an invoice, essentially. Six months from now,
     * a customer looking at their order history needs to see what they
     * actually bought, even if that product has since been renamed,
     * discontinued, or deleted entirely from the catalog. A cart is
     * transient and still-editable (fetching the live name via Feign
     * made sense); an order is a frozen receipt (fetching the name live
     * would mean historical orders could show WRONG information if the
     * product was later renamed — clearly worse for a receipt than a
     * frozen snapshot). Same underlying "what does freshness mean for
     * this field" question from Phase F, landing on the opposite answer
     * because the surrounding context (mutable cart vs. permanent
     * record) is different.
     */
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "price_at_purchase", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtPurchase;

    public OrderItem(Long productId, String productName, Integer quantity, BigDecimal priceAtPurchase){
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.priceAtPurchase = priceAtPurchase;
    }

    public BigDecimal getSubTotal(){
        return priceAtPurchase.multiply(BigDecimal.valueOf(quantity));
    }
}
