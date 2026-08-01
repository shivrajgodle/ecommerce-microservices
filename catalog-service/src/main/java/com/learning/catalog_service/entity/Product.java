package com.learning.catalog_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "products",
        // Indexes speed up reads on columns you filter/sort by frequently.
        // We KNOW category-based browsing and SKU lookups will be common
        // access patterns for a catalog, so we declare them explicitly here
        // rather than discovering the need later via a slow-query log.
        indexes = {
                @Index(name = "idx_product_category", columnList = "category_id"),
                @Index(name = "idx_product_sku", columnList = "sku", unique = true)
        }
)
public class Product extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    /**
     * BigDecimal, never double/float, for money. Floating-point binary
     * representation cannot exactly represent most decimal fractions
     * (0.1 + 0.2 != 0.3 in IEEE 754) — a rounding error that's merely
     * annoying in most domains becomes an actual accounting problem
     * with prices. precision=10, scale=2 means up to 8 digits before
     * the decimal point and exactly 2 after (e.g. 99999999.99).
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * OPTIMISTIC LOCKING. Once Cart Service and Order Service exist,
     * multiple concurrent checkouts could try to decrement this same
     * product's stock at nearly the same instant. @Version adds a
     * column Hibernate auto-increments on every UPDATE, and includes in
     * the WHERE clause of every update it issues (UPDATE products SET
     * stock_quantity=?, version=? WHERE id=? AND version=?). If another
     * transaction updated this row first (version already moved on),
     * this update matches zero rows, and Hibernate throws
     * OptimisticLockException — telling YOUR code "someone else changed
     * this between when you read it and when you tried to write it,
     * retry or fail the operation" instead of silently overwriting
     * their change or corrupting the stock count. We'll actually
     * exercise this properly once Order Service does concurrent stock
     * decrements — flagging it here since it belongs on the entity from
     * the start, not bolted on later.
     */
    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * Product is the OWNING side this time (contrast with Phase 1, where
     * User owned the User<->Role join) — a deliberate choice: tagging a
     * product ("add tags to this product") is the natural direction this
     * relationship is modified from in this domain, the same reasoning
     * we used to decide User owned Role.
     *
     * Set, not List — this matters more than it looks. Hibernate can
     * throw MultipleBagFetchException if an entity has more than one
     * List-typed collection fetched eagerly/joined in the same query
     * (a "bag" is Hibernate's term for a List without unique-element
     * semantics — it can't safely deduplicate rows from a SQL JOIN the
     * way a Set can). Using Set here avoids that entire class of bug
     * before it can happen, and also correctly reflects that a product
     * shouldn't have the same tag twice.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_tags",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    public Product(String sku, String name, String description, BigDecimal price,
                   Integer stockQuantity, Category category) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.category = category;
    }

    public void addTag(Tag tag){
        this.tags.add(tag);
    }

    public void removeTag(Tag tag){
        this.tags.remove(tag);
    }
}
