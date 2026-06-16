package com.bootsignal.domain.tech_article.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TechArticleFilterKeywords 테스트")
class TechArticleFilterKeywordsTest {

	@Test
	@DisplayName("제목에 키워드가 포함되면 매칭된다")
	void matchesWhenTitleContainsKeyword() {
		assertThat(TechArticleFilterKeywords.matchesTitle("백엔드 개발자 취업 준비 팁")).isTrue();
		assertThat(TechArticleFilterKeywords.matchesTitle("주니어 실무 가이드")).isTrue();
	}

	@Test
	@DisplayName("제목에 키워드가 없으면 매칭되지 않는다")
	void doesNotMatchWhenKeywordAbsent() {
		assertThat(TechArticleFilterKeywords.matchesTitle("요즘 트렌드")).isFalse();
		assertThat(TechArticleFilterKeywords.matchesTitle("디자인 시스템 구축기")).isFalse();
	}

	@Test
	@DisplayName("커스텀 키워드 목록으로 매칭할 수 있다")
	void matchesWithCustomKeywords() {
		assertThat(TechArticleFilterKeywords.matchesTitle("제목", List.of("테스트"))).isFalse();
		assertThat(TechArticleFilterKeywords.matchesTitle("테스트 제목", List.of("테스트"))).isTrue();
	}
}
