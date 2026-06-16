package com.bootsignal.domain.review.dto;

import com.bootsignal.domain.review.entity.ReviewType;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 리뷰 작성 요청을 받는 DTO이며, 인증 리뷰는 상세 설문을 함께 전달받습니다.
 */
public record ReviewCreateRequest(
    @NotNull Long courseSessionId,
    @NotNull ReviewType reviewType,
    @JsonAlias("overallRating")
    @Min(1) @Max(5) Integer rating,
    String content,
    @Valid VerifiedReviewDetailRequest verifiedDetail
) {}
