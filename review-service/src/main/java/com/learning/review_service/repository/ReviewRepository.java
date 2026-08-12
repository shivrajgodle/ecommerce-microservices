package com.learning.review_service.repository;



import com.learning.review_service.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByProductId(Long productId, Pageable pageable);

    boolean existsByProductIdAndUserId(Long productId, Long userId);

    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);

    // JPQL aggregate — returns a plain Double, not an entity. AVG over
    // zero rows returns SQL NULL, which is why the service layer
    // (Step 9) needs to handle a null result explicitly rather than
    // assuming a product with no reviews yet returns 0.0.
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.productId = :productId")
    Double findAverageRatingByProductId(@Param("productId") Long productId);

    long countByProductId(Long productId);
}