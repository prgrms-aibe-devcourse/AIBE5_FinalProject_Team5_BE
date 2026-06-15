package com.bootsignal.domain.ai.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 크롤링된 수강후기 요약은 과정 ID와 요약에 사용할 최대 후기 수를 입력받는다.
public record ReviewSummaryCreateRequest(
	@NotNull Long courseId,
	@Min(1) @Max(200) Integer maxReviewCount
) {
	private static final int DEFAULT_MAX_REVIEW_COUNT = 50;

	public int resolvedMaxReviewCount() {
		return maxReviewCount == null ? DEFAULT_MAX_REVIEW_COUNT : maxReviewCount;
	}
}
