package com.bootsignal.domain.crawled_review.dto;

import com.bootsignal.domain.crawled_review.entity.CrawledReview;
import com.bootsignal.domain.crawled_review.entity.CrawledReviewSource;

import java.time.Instant;
import java.time.LocalDateTime;

public record CrawledReviewResponse(
        Long id,
        CrawledReviewSource source,
        String reviewerNickname,
        Integer rating,
        String content,
        LocalDateTime reviewedAt,
        Instant crawledAt
) {
    public static CrawledReviewResponse from(CrawledReview review) {
        return new CrawledReviewResponse(
                review.getId(),
                review.getSource(),
                review.getReviewerNickname(),
                review.getRating(),
                review.getContent(),
                review.getReviewedAt(),
                review.getCrawledAt()
        );
    }
}
