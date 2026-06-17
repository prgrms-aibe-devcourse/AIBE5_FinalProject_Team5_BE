package com.bootsignal.domain.tech_article.filter;

import java.util.List;
import java.util.Locale;
import org.springframework.util.StringUtils;

/**
 * KDT 예비 수강생·취업·커리어 발전 타겟 사용자를 위한 RSS 아티클 제목 키워드 필터.
 */
public final class TechArticleFilterKeywords {

	private TechArticleFilterKeywords() {
	}

	private static final List<String> KEYWORDS = List.of(
		"개발",
		"프로그래밍",
		"백엔드",
		"프론트엔드",
		"비전공자",
		"비개발자",
		"클라우드",
		"취업",
		"커리어",
		"주니어",
		"알고리즘",
		"면접",
		"포트폴리오",
		"신입",
		"이력서",
		"사이드프로젝트",
		"실무",
		"협업",
		"성장",
		"학습",
		"문제",
		"해결",
		"역량"
	);

	// 제목에 키워드가 포함되면 true
	public static boolean matchesTitle(String title) {
		if (!StringUtils.hasText(title)) {
			return false;
		}
		String normalizedTitle = title.toLowerCase(Locale.ROOT);
		return KEYWORDS.stream()
			.map(keyword -> keyword.toLowerCase(Locale.ROOT))
			.anyMatch(normalizedTitle::contains);
	}

	// 테스트용 커스텀 키워드 매칭
	static boolean matchesTitle(String title, List<String> keywords) {
		if (keywords == null || keywords.isEmpty()) {
			return true;
		}
		if (!StringUtils.hasText(title)) {
			return false;
		}
		String normalizedTitle = title.toLowerCase(Locale.ROOT);
		return keywords.stream()
			.filter(StringUtils::hasText)
			.map(keyword -> keyword.toLowerCase(Locale.ROOT))
			.anyMatch(normalizedTitle::contains);
	}
}
