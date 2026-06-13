package com.bootsignal.domain.ai.review.tool;

import java.math.BigDecimal;
import java.util.List;

// Agent 내부 tool들이 공유하는 정규화된 수강후기 요약 입력 모델이다.
public record ReviewSummaryInput(
	Long courseId,
	String courseTitle,
	int reviewCount,
	BigDecimal averageRating,
	List<CrawledReviewSnippet> reviews
) {
	public ReviewSummaryInput {
		averageRating = averageRating == null ? BigDecimal.ZERO : averageRating;
		reviews = reviews == null ? List.of() : List.copyOf(reviews);
	}
}
