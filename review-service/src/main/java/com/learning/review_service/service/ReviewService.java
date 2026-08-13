package com.learning.review_service.service;

import com.learning.review_service.dto.request.ReviewRequest;
import com.learning.review_service.dto.request.ReviewUpdateRequest;
import com.learning.review_service.dto.response.RatingSummaryResponse;
import com.learning.review_service.dto.response.ReviewResponse;
import com.learning.review_service.entity.Review;
import com.learning.review_service.exception.DuplicateResourceException;
import com.learning.review_service.exception.ForbiddenOperationException;
import com.learning.review_service.exception.ResourceNotFoundException;
import com.learning.review_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    @Transactional
    public ReviewResponse createReview(Long userId, ReviewRequest request){
        // Layer 1 of protection — the fast, friendly path. Catches the
        // overwhelmingly common case (user genuinely hasn't reviewed
        // this yet, or is retrying after seeing a clean error) with a
        // clear, specific message before ever touching the database's
        // constraint.
        if(reviewRepository.existsByProductIdAndUserId(request.getProductId(),userId)){
            throw new DuplicateResourceException("You have already reviewed the product");
        }

        // Layer 2 — the composite unique constraint from File 1 — is
        // what actually closes the race condition if two requests from
        // the same user for the same product land concurrently and
        // both pass the check above before either commits. That
        // failure surfaces as DataIntegrityViolationException, handled
        // globally (Step 6) with the same user-facing message — the
        // caller can't tell which layer caught it, and doesn't need to.
        Review review = new Review(request.getProductId(),userId,request.getRating(),request.getComment());
        Review saved = reviewRepository.save(review);
        return toResponse(saved);
    }

    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, ReviewUpdateRequest request) {
        Review review = getReviewOrThrow(reviewId);

        // THE AUTHORIZATION CHECK. Authentication (Phase D) already told
        // us this IS a real, logged-in user — this line is the separate
        // question of whether THIS user may act on THIS resource.
        // Note this is a plain 'if' + custom exception, not a framework
        // annotation like @PreAuthorize — a completely legitimate,
        // simpler alternative to method security for straightforward,
        // single-condition ownership checks like this one.
        if (!review.getUserId().equals(userId)) {
            throw new ForbiddenOperationException("You can only edit your own reviews");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        Review saved = reviewRepository.save(review);
        return toResponse(saved);
    }

    @Transactional
    public void deleteReview(Long userId, List<String> userRoles, Long reviewId) {
        Review review = getReviewOrThrow(reviewId);

        boolean isOwner = review.getUserId().equals(userId);
        boolean isAdmin = userRoles != null && userRoles.contains("ROLE_ADMIN");

        // A SECOND, DIFFERENT authorization rule from updateReview above
        // — deletion allows EITHER ownership OR an admin role (content
        // moderation is a real, common requirement: an admin needs to
        // remove an abusive review even though they didn't write it).
        // This is exactly why we forward X-Auth-User-Roles from the
        // gateway (built all the way back in Phase D, File 2) even
        // though no service had actually READ it until now — this is
        // its first real use in the whole project.
        if (!isOwner && !isAdmin) {
            throw new ForbiddenOperationException(
                    "You can only delete your own reviews (or be an admin)");
        }

        reviewRepository.delete(review);
    }

    public Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable).map(this::toResponse);
    }

    public RatingSummaryResponse getRatingSummary(Long productId) {
        Double avg = reviewRepository.findAverageRatingByProductId(productId);
        long count = reviewRepository.countByProductId(productId);

        return RatingSummaryResponse.builder()
                .productId(productId)
                // The null-safety payoff from File 1's Double discussion
                // — a product with zero reviews correctly shows 0.0, not
                // a NullPointerException from unboxing a null Double.
                .averageRating(avg != null ? avg : 0.0)
                .reviewCount(count)
                .build();
    }

    private Review getReviewOrThrow(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProductId())
                .userId(review.getUserId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdDate(review.getCreatedDate())
                .build();
    }
}
