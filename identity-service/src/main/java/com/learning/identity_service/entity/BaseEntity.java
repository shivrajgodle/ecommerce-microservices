package com.learning.identity_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Common fields shared by every entity in the system: id, audit
 * timestamps, and a soft-delete flag.
 *
 * @MappedSuperclass (NOT @Entity) — this class has NO table of its own.
 * Its fields are merged into each subclass's table as regular columns.
 * This is the key distinction vs @Entity + table-per-class inheritance:
 * we're not modeling an IS-A relationship for querying purposes, we're
 * just sharing field definitions. If you queried "all BaseEntities" you
 * couldn't — there's no such table.
 */

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    /**
     * IDENTITY strategy delegates PK generation to the database's
     * native auto-increment (Postgres SERIAL/IDENTITY column).
     * Alternative is SEQUENCE (a separate DB sequence object, allows
     * batching inserts more efficiently — worth knowing for interviews
     * even though IDENTITY is simpler and what we'll use here).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /**
     * Populated automatically ON INSERT by AuditingEntityListener,
     * which intercepts the JPA @PrePersist lifecycle callback.
     * updatable = false means even if code accidentally reassigns this
     * field later, Hibernate will never write a changed value to it.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Populated on both INSERT and every UPDATE, via @PreUpdate.
     */
    @LastModifiedDate
    @Column(name = "modified_date", nullable = false)
    private LocalDateTime modifiedAt;

    /**
     * Soft delete flag. We never actually DELETE rows for most entities
     * in this project (Order, Product, User, etc.) — we flip this flag
     * instead. Real reasons this matters in production: preserving
     * order history for accounting/legal reasons, undo capability,
     * and referential integrity (a "deleted" Product might still be
     * referenced by 500 historical OrderItems).
     */
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;
}
