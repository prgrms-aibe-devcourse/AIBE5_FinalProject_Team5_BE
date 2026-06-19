package com.bootsignal.domain.review.dto;

import com.bootsignal.domain.review.entity.Review;
import com.bootsignal.domain.review.entity.ReviewType;
import java.time.LocalDateTime;

/**
 * 리뷰 기본 정보, 작성자 프로필, 과정 정보와 인증 리뷰 상세 설문을 함께 내려주는 응답 DTO입니다.
 */
public record ReviewResponse(
    Long reviewId,
    Long userId,
    String userNickname,
    String userProfileImageUrl,
    Long courseId,
    String courseTitle,
    Long courseSessionId,
    ReviewType reviewType,
    Integer rating,
    String content,
    VerifiedReviewDetailResponse verifiedDetail,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
            review.getId(),
            review.getUser().getId(),
            review.getUser().getNickname(),
            review.getUser().getProfileImageUrl(),
            review.getCourse().getId(),
            review.getCourse().getTitle(),
            review.getCourseSession().getId(),
            review.getReviewType(),
            review.getRating(),
            review.getContent(),
            VerifiedReviewDetailResponse.from(review.getVerifiedDetail()),
            review.getCreatedAt(),
            review.getUpdatedAt()
        );
    }
}
