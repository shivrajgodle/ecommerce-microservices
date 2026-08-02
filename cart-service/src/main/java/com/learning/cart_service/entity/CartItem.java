package com.learning.cart_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id",nullable = false)
    private Cart cart;

    /**
     * Another cross-service reference — same reasoning as Cart.userId
     * above. This is just a number; the actual Product lives in
     * catalog_db. To know this product's current name/price/stock, this
     * service has to ASK Catalog Service (the Feign call, File 3) — it
     * cannot JOIN to find out.
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    /**
     * DENORMALIZED price snapshot — the exact concept flagged back in
     * Phase C File 2's discussion of cross-service data. We store the
     * price AT THE MOMENT this item was added, fetched via Feign from
     * Catalog Service and cached here. Why not just always call Catalog
     * Service live for the price whenever we need it? Two reasons:
     * (1) performance — displaying a cart shouldn't require N outbound
     * calls for N items on every single view; (2) correctness — if the
     * product's price changes AFTER it's in someone's cart, most retail
     * systems intentionally show the price the user saw when they added
     * it, not a silently-changed one, until they actually check out
     * (where Order Service will look up the CURRENT price again — a
     * distinction worth being explicit about when we build that).
     */
    @Column(name = "price_snapshot", nullable = false,precision = 10,scale = 2)
    private BigDecimal priceSnapshot;

    public CartItem(Long productId, Integer quantity, BigDecimal priceSnapshot){
        this.productId = productId;
        this.quantity = quantity;
        this.priceSnapshot = priceSnapshot;
    }

    public BigDecimal getSubtotal(){
        return priceSnapshot.multiply(BigDecimal.valueOf(quantity));
    }

}
