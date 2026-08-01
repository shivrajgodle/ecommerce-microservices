package com.learning.catalog_service.repository.spec;

import com.learning.catalog_service.entity.Product;
import com.learning.catalog_service.entity.Tag;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

/**
 * A Specification<Product> is, under the hood, a functional interface
 * wrapping the JPA Criteria API — a programmatic, type-safe way to build
 * SQL WHERE clauses in Java rather than string-concatenating JPQL. Each
 * method here returns ONE reusable condition; they get combined with
 * .and()/.or() at the call site (Step 4's service layer), meaning any
 * combination of filters composes cleanly without a combinatorial
 * explosion of repository methods (imagine trying to write a derived
 * query method for every possible subset of 4 optional filters — 16
 * method names. Specifications solve exactly this.)
 */
public class ProductSpecifications {

    public static Specification<Product> hasCategoryId(Long categoryId){
        // Returning null here is significant: Spring Data's
        // Specification.and() treats a null predicate as "no
        // condition", so this filter is simply omitted from the WHERE
        // clause entirely when categoryId wasn't provided — rather than
        // us needing separate if/else branches to conditionally build
        // the query.
        return (root, query, criteriaBuilder) -> categoryId == null ? null : criteriaBuilder.equal(root.get("category").get("id"),categoryId);
    }

    public static Specification<Product> priceBetween(BigDecimal min , BigDecimal max){
        return (root, query, cb) -> {
            if(min == null & max == null) return null;
            if(min == null) return cb.lessThanOrEqualTo(root.get("price"),max);
            if(max == null) return cb.greaterThanOrEqualTo(root.get("price"),min);
            return  cb.between(root.get("price"),min,max);
        };
    }

    public static Specification<Product> nameContains(String keyword){
        return (root, query, cb) ->
            (keyword == null || keyword.isBlank()) ? null : cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%" );
    }

    public static Specification<Product> hasAnyTag(List<String> tagNames){
        return (root, query, cb) -> {
            if(tagNames == null || tagNames.isEmpty()) return null;

            // DISTINCT matters here: joining Product -> Tag can multiply
            // rows (a product with 3 matching tags would otherwise
            // appear 3 times in the result set before this).
            query.distinct(true);

            Join<Product, Tag> tagJoin = root.join("tags");
            return tagJoin.get("name").in(tagNames);
        };
    }

    public static Specification<Product> isActive(){
        return ((root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("active")))
    }

}

