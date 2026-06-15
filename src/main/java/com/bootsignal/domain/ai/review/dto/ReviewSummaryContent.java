package com.bootsignal.domain.ai.review.dto;

import java.util.List;

// AI가 생성한 수강후기 요약 본문을 화면에서 바로 쓰기 쉬운 구조로 담는다.
public record ReviewSummaryContent(
	String summary,
	List<String> strengths,
	List<String> weaknesses,
	List<String> recommendedFor,
	List<String> keywords
) {
	public ReviewSummaryContent {
		strengths = strengths == null ? List.of() : List.copyOf(strengths);
		weaknesses = weaknesses == null ? List.of() : List.copyOf(weaknesses);
		recommendedFor = recommendedFor == null ? List.of() : List.copyOf(recommendedFor);
		keywords = keywords == null ? List.of() : List.copyOf(keywords);
	}
}
