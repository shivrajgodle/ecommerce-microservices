package com.learning.review_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "reviews",
        indexes = {
                @Index(name = "idx_review_product", columnList = "product_id")
        },
        // COMPOSITE unique constraint — different from every single-column
        // unique = true we've used so far (User.email, Product.sku, etc.).
        // This says: the COMBINATION of product_id + user_id must be
        // unique, even though neither column is unique on its own (one
        // user reviews many products; one product has many reviewers). The
        // business rule this enforces — "one review per user per product"
        // — lives at the DATABASE level here, not just checked in service
        // code, for the same reason every other constraint we've added
        // does: application-level checks can race under concurrent
        // requests, a database constraint physically cannot be violated no
        // matter how many requests arrive simultaneously.
        uniqueConstraints = @UniqueConstraint(
                name = "uq_review_product_user", columnNames = {"product_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class Review extends BaseEntity {

    // Cross-service reference, same pattern throughout this project —
    // no @ManyToOne, no FK, Catalog Service owns the real Product.
    @Column(name = "product_id", nullable = false)
    private Long productId;

    // Cross-service reference to Identity Service's User — same reasoning.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer rating; // 1-5, enforced by DTO validation (Step 8), not the entity itself

    @Column(length = 1000)
    private String comment;

    public Review(Long productId, Long userId, Integer rating, String comment) {
        this.productId = productId;
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
    }
}