package com.learning.review_service.controller;

import com.learning.review_service.dto.request.ReviewRequest;
import com.learning.review_service.dto.request.ReviewUpdateRequest;
import com.learning.review_service.dto.response.ApiResponse;
import com.learning.review_service.dto.response.RatingSummaryResponse;
import com.learning.review_service.dto.response.ReviewResponse;
import com.learning.review_service.security.CurrentUserId;
import com.learning.review_service.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> create(@CurrentUserId Long userId, @Valid @RequestBody ReviewRequest request) {
        ReviewResponse result = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(HttpStatus.CREATED.value(), "Review created", result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewResponse>> update(
            @CurrentUserId Long userId, @PathVariable Long id,
            @Valid @RequestBody ReviewUpdateRequest request) {
        ReviewResponse result = reviewService.updateReview(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Review updated", result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @CurrentUserId Long userId,
            // Read directly here, rather than via a dedicated resolver
            // like @CurrentUserId — this is the ONLY endpoint in the
            // whole project so far that needs roles, so a one-off
            // @RequestHeader is proportionate. If a THIRD service
            // needed this, that would be the signal to extract a
            // @CurrentUserRoles resolver the same way we did for
            // userId — a good instinct generally: duplicate a small
            // thing twice before you generalize it, not before.
            @RequestHeader(value = "X-Auth-User-Roles", required = false) String rolesHeader,
            @PathVariable Long id) {
        List<String> roles = rolesHeader != null ? Arrays.asList(rolesHeader.split(",")) : Collections.emptyList();
        reviewService.deleteReview(userId, roles, id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Review deleted", null));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewResponse>> getByProduct(
            @PathVariable Long productId,
            @PageableDefault(size = 10, sort = "createdDate") Pageable pageable) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId, pageable));
    }

    @GetMapping("/product/{productId}/summary")
    public ResponseEntity<ApiResponse<RatingSummaryResponse>> getSummary(@PathVariable Long productId) {
        RatingSummaryResponse result = reviewService.getRatingSummary(productId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Rating summary retrieved", result));
    }
}