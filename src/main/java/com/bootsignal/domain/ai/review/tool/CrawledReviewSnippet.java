package com.bootsignal.domain.ai.review.tool;

// 요약 프롬프트에 넣을 수 있도록 크롤링 후기의 핵심 필드만 보관한다.
public record CrawledReviewSnippet(
	Long id,
	Integer rating,
	String content
) {
}
