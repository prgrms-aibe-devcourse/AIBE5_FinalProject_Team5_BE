package com.bootsignal.domain.tech_article.dto;

import com.bootsignal.domain.tech_article.entity.ArticleSource;
import java.time.LocalDateTime;

/* RSS 피드에서 파싱한 기술 아티클 정보 */
public record ParsedRssArticle(
	String title,
	String summary,
	String thumbnailUrl,
	String author,
	String articleUrl,
	LocalDateTime publishedAt,
	String rssGuid
) {

	public static ParsedRssArticle of(
		String title,
		String summary,
		String thumbnailUrl,
		String author,
		String articleUrl,
		LocalDateTime publishedAt,
		String rssGuid
	) {
		return new ParsedRssArticle(
			trimToNull(title),
			trimToNull(summary),
			trimToNull(thumbnailUrl),
			trimToNull(author),
			requireNonBlank(articleUrl, "articleUrl"),
			publishedAt,
			requireNonBlank(rssGuid, "rssGuid")
		);
	}

	// 문자열 공백 제거 및 공백 검증
	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	// 문자열 null 또는 공백 예외 발생
	private static String requireNonBlank(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.trim();
	}
}
