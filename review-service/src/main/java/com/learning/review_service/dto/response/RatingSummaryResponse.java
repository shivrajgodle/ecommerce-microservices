package com.learning.review_service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RatingSummaryResponse {
    private Long productId;
    private double averageRating;
    private long reviewCount;
}