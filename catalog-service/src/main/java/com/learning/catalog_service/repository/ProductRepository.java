package com.learning.catalog_service.repository;

import com.learning.catalog_service.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Extending JpaSpecificationExecutor<Product> alongside JpaRepository is
 * what unlocks the Specification-based dynamic filtering in Step 3 below
 * — it's a second, separate interface Spring Data stitches into the
 * same repository proxy, adding findAll(Specification, Pageable) and
 * similar overloads on top of the usual CRUD methods.
 */
public interface ProductRepository extends JpaRepository<Product,Long>,
        JpaSpecificationExecutor<Product> {

    // ============================================================
    // DERIVED QUERY METHODS
    // Spring Data parses the METHOD NAME at application startup and
    // generates the query — no implementation code, no annotation.
    // Great for simple, fixed-shape queries; gets unwieldy fast for
    // anything with more than 2-3 conditions (see the "common mistakes"
    // section for exactly where this breaks down).
    // ============================================================

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    // "By" + field name + implicit equality. Returns a Page because
    // category browsing needs pagination — see Step 4.
    Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable page);

    // Keyword "Between" maps to a SQL BETWEEN clause.
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // "ContainingIgnoreCase" -> SQL LIKE '%value%' with case-insensitive
    // comparison. Fine for a small catalog; NOT how you'd implement
    // real full-text search at scale (that's what Elasticsearch/
    // pg_trgm/full-text-search extensions are for — worth knowing this
    // approach's ceiling, not just its existence).
    List<Product> findByNameContainingIgnoreCase(String name);

    // Derived queries can traverse relationships too — this generates a
    // JOIN to categories under the hood, purely from the method name.
    List<Product> findByCategory_Name(String categoryName);

    // ============================================================
    // JPQL — @Query written against ENTITY fields, not table/column
    // names. Hibernate translates this into SQL at runtime. Reach for
    // this when a derived-query method name would become unreadably
    // long, or when you need something derived-query syntax can't
    // express (subqueries, specific projections, etc.).
    // ============================================================

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stockQuantity > 0" + "AND p.category.id = :categoryId ORDER BY p.name ASC")
    List<Product>  findAvailableProductsByCategories(@Param("categoryId") Long categoryId);

    // JPQL aggregate query — returns a scalar, not entities.
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId AND p.active = true")
    Long countActiveProductsInCategory(@Param("categoryId") Long categoryId);

    // ============================================================
    // NATIVE QUERY — actual raw SQL, runs directly against Postgres,
    // bypassing JPQL/Hibernate's entity-query translation entirely.
    // Reach for this when you need DB-specific functionality JPQL has
    // no vocabulary for — here, Postgres's own full-text search
    // (to_tsvector/plainto_tsquery), which has no JPQL equivalent.
    // ============================================================

    @Query(value = "SELECT * FROM products p WHERE "+ "to_tsvector('english',p.name || ' ' || coalesce(p.description,''))" + "@@ plainto_tsquery('english',:searchTerm) AND p.active = true", nativeQuery = true)
    List<Product> fullTextSearch(@Param("searchTerm") String searchTerm);


    // ============================================================
    // BULK UPDATE via JPQL — @Modifying is REQUIRED on any @Query that
    // isn't a SELECT. Without it, Spring Data assumes read-only and
    // throws at runtime. This is a genuinely different execution path
    // from loading entities into the persistence context and saving
    // them individually — it issues ONE SQL UPDATE statement affecting
    // every matching row directly, bypassing the persistence context
    // and any @Version optimistic-lock checks entirely. Use deliberately,
    // not as a default habit — covered further in "common mistakes".
    // ============================================================
    @Modifying
    @Query("UPDATE Product p SET p.active = false WHERE p.category.id = :categoryId")
    int deactivateAllProductsInCategory(@Param("categoryId") Long categoryId);
}
