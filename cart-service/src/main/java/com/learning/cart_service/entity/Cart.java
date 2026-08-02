package com.learning.cart_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Table(name = "carts")
@NoArgsConstructor
@Entity
public class Cart extends BaseEntity{

    /**
     * THIS is the cross-service reference pattern from Phase C, File 2,
     * now showing up for real: just a Long, not a User object. There is
     * NO @ManyToOne here, NO @JoinColumn, no foreign key constraint at
     * the database level — Postgres has no way to verify this userId
     * actually exists, because the users table lives in a completely
     * different database (identity_db) that this service can't even
     * see. Referential integrity for this relationship is enforced by
     * application logic only (we trust the X-Auth-User-Id header the
     * gateway forwards, from Phase D File 2), not by the schema.
     */

    @Column(name = "user_id", nullable = false, unique = true)
    public Long userId;

    /**
     * One cart per user, enforced by that unique = true above — this is
     * our One-to-One (in spirit; there's no @OneToOne annotation because
     * the "other side" — User — lives in a different service entirely
     * and can't be a JPA relationship at all).
     */
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL,orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    public Cart(Long userId){
        this.userId = userId;
    }

    public void addItem(CartItem item){
        items.add(item);
        item.setCart(this);
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }

    /**
     * Small domain helper — checks whether this cart already has a line
     * for a given product, so the service layer can decide "increment
     * quantity" vs "add new line" without reaching into the collection
     * itself. Keeping small, genuinely cart-specific logic like this ON
     * the entity (rather than purely in the service) is a defensible
     * choice — it's intrinsic to what a Cart IS, not an external
     * business process acting on it. Worth knowing this is a judgment
     * call, not a hard rule — heavier logic still belongs in the service.
     */
    public CartItem findItemByProductId(Long productId){
        return items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }
}
