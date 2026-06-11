package com.bootsignal.domain.review.dto;

import com.bootsignal.domain.review.entity.Review;
import com.bootsignal.domain.review.entity.ReviewType;
import java.time.LocalDateTime;

public record ReviewResponse(
    Long reviewId,
    Long userId,
    String userNickname,
    Long courseId,
    Long courseSessionId,
    ReviewType reviewType,
    Integer rating,
    String content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
            review.getId(),
            review.getUser().getId(),
            review.getUser().getNickname(),
            review.getCourse().getId(),
            review.getCourseSession().getId(),
            review.getReviewType(),
            review.getRating(),
            review.getContent(),
            review.getCreatedAt(),
            review.getUpdatedAt()
        );
    }
}
