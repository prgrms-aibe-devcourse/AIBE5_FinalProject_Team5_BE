package com.bootsignal.domain.tech_article.service;

import com.bootsignal.domain.tech_article.config.TechArticleRssProperties;
import com.bootsignal.domain.tech_article.dto.ParsedRssArticle;
import com.bootsignal.domain.tech_article.dto.TechArticleCollectResponse;
import com.bootsignal.domain.tech_article.dto.TechArticleCollectResponse.SourceCollectResult;
import com.bootsignal.domain.tech_article.entity.ArticleSource;
import com.bootsignal.domain.tech_article.entity.TechArticle;
import com.bootsignal.domain.tech_article.filter.TechArticleFilterKeywords;
import com.bootsignal.domain.tech_article.repository.TechArticleRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TechArticleCollectService {

	private final RssFeedParser rssFeedParser;
	private final TechArticleRepository techArticleRepository;
	private final TechArticleRssProperties rssProperties;

	/* RSS 피드에서 기술 아티클을 수집 */
	@Transactional
	public TechArticleCollectResponse collect(ArticleSource source) {
		List<ArticleSource> targets = source == null
			? List.of(ArticleSource.values())
			: List.of(source);

		List<SourceCollectResult> results = targets.stream()
			.map(this::collectSource)
			.toList();

		return new TechArticleCollectResponse(results);
	}

	// 소스별 아티클 수집
	private SourceCollectResult collectSource(ArticleSource source) {
		List<ParsedRssArticle> fetchedArticles;
		try {
			fetchedArticles = rssFeedParser.fetchAndParse(source);
		} catch (IOException e) {
			throw new BootSignalException(ErrorCode.INTERNAL_SERVER_ERROR,
				source + " RSS 피드를 가져오지 못했습니다: " + e.getMessage());
		}

		List<ParsedRssArticle> keywordMatched = fetchedArticles.stream()
			.filter(article -> isWithinCollectPeriod(article.publishedAt()))
			.filter(article -> TechArticleFilterKeywords.matchesTitle(article.title()))
			.sorted(Comparator.comparing(ParsedRssArticle::publishedAt).reversed())
			.toList();

		List<ParsedRssArticle> articlesToSave = keywordMatched.stream()
			.limit(rssProperties.collectLimit())
			.toList();

		int inserted = 0;
		int updated = 0;
		for (ParsedRssArticle article : articlesToSave) {
			var existingOpt = techArticleRepository.findBySourceAndRssGuid(source, article.rssGuid());
			if (existingOpt.isPresent()) {
				TechArticle existing = existingOpt.get();
				existing.updateFromRss(
					article.title(),
					article.summary(),
					article.thumbnailUrl(),
					article.author(),
					article.articleUrl(),
					article.publishedAt()
				);
				updated++;
			} else {
				techArticleRepository.save(article.toEntity(source));
				inserted++;
			}
		}

		int filteredOut = fetchedArticles.size() - keywordMatched.size();
		return new SourceCollectResult(
			source,
			inserted,
			updated,
			fetchedArticles.size(),
			filteredOut,
			keywordMatched.size()
		);
	}

	// 발행일이 수집 기준 개월 이내인지 확인
	private boolean isWithinCollectPeriod(LocalDateTime publishedAt) {
		LocalDateTime publishedAfter = LocalDateTime.now()
			.minusMonths(rssProperties.collectWithinMonths());
		return !publishedAt.isBefore(publishedAfter);
	}
}
