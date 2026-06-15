package com.bootsignal.domain.ai.review.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// API 응답에는 실행 ID와 과정 정보, AI 요약 결과만 노출한다.
public record ReviewSummaryResponse(
	UUID executionId,
	Long courseId,
	String courseTitle,
	int reviewCount,
	BigDecimal averageRating,
	String summary,
	List<String> strengths,
	List<String> weaknesses,
	List<String> recommendedFor,
	List<String> keywords
) {
	public static ReviewSummaryResponse from(
		UUID executionId,
		Long courseId,
		String courseTitle,
		int reviewCount,
		BigDecimal averageRating,
		ReviewSummaryContent content
	) {
		return new ReviewSummaryResponse(
			executionId,
			courseId,
			courseTitle,
			reviewCount,
			averageRating,
			content.summary(),
			content.strengths(),
			content.weaknesses(),
			content.recommendedFor(),
			content.keywords()
		);
	}
}
