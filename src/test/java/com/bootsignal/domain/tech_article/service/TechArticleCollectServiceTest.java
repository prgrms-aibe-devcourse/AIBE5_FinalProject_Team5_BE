package com.bootsignal.domain.tech_article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bootsignal.domain.tech_article.config.TechArticleRssProperties;
import com.bootsignal.domain.tech_article.dto.ParsedRssArticle;
import com.bootsignal.domain.tech_article.entity.ArticleSource;
import com.bootsignal.domain.tech_article.repository.TechArticleRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TechArticleCollectService 테스트")
class TechArticleCollectServiceTest {

	@Mock
	private RssFeedParser rssFeedParser;

	@Mock
	private TechArticleRepository techArticleRepository;

	@Mock
	private TechArticleRssProperties rssProperties;

	@InjectMocks
	private TechArticleCollectService techArticleCollectService;

	@Test
	@DisplayName("발행일이 수집 기준 6개월을 초과한 글은 저장하지 않는다")
	void doesNotSaveArticlesOlderThanCollectWithinMonths() throws IOException {
		when(rssProperties.collectWithinMonths()).thenReturn(6);
		when(rssProperties.collectLimit()).thenReturn(30);
		when(techArticleRepository.findBySourceAndRssGuid(any(), any())).thenReturn(Optional.empty());

		ParsedRssArticle recentArticle = article("recent", LocalDateTime.now().minusMonths(1));
		ParsedRssArticle oldArticle = article("old", LocalDateTime.now().minusMonths(7));
		when(rssFeedParser.fetchAndParse(ArticleSource.YOZM)).thenReturn(List.of(recentArticle, oldArticle));

		var response = techArticleCollectService.collect(ArticleSource.YOZM);

		assertThat(response.results()).hasSize(1);
		assertThat(response.results().getFirst().inserted()).isEqualTo(1);
		assertThat(response.results().getFirst().filteredOut()).isEqualTo(1);
		verify(techArticleRepository).save(any());
	}

	@Test
	@DisplayName("발행일이 수집 기준 6개월 이내인 글만 keywordMatched에 포함된다")
	void includesOnlyRecentArticlesInKeywordMatchedCount() throws IOException {
		when(rssProperties.collectWithinMonths()).thenReturn(6);
		when(rssProperties.collectLimit()).thenReturn(30);
		when(techArticleRepository.findBySourceAndRssGuid(any(), any())).thenReturn(Optional.empty());

		ParsedRssArticle recentArticle = article("recent", LocalDateTime.now().minusMonths(2));
		ParsedRssArticle oldArticle = article("old", LocalDateTime.now().minusMonths(8));
		when(rssFeedParser.fetchAndParse(ArticleSource.KAKAO_TECH))
			.thenReturn(List.of(recentArticle, oldArticle));

		var response = techArticleCollectService.collect(ArticleSource.KAKAO_TECH);

		assertThat(response.results().getFirst().keywordMatched()).isEqualTo(1);
		assertThat(response.results().getFirst().inserted()).isEqualTo(1);
		assertThat(response.results().getFirst().totalFetched()).isEqualTo(2);
		verify(techArticleRepository).save(any());
	}

	private static ParsedRssArticle article(String key, LocalDateTime publishedAt) {
		return ParsedRssArticle.of(
			"백엔드 " + key,
			"summary",
			null,
			null,
			"https://example.com/" + key,
			publishedAt,
			"guid-" + key
		);
	}
}
