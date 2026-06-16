package com.bootsignal.domain.review.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 리뷰 수정 요청을 받는 DTO이며, 인증 리뷰는 상세 설문도 갱신할 수 있습니다.
 */
public record ReviewUpdateRequest(
    @JsonAlias("overallRating")
    @Min(1) @Max(5) Integer rating,
    String content,
    @Valid VerifiedReviewDetailRequest verifiedDetail
) {}
