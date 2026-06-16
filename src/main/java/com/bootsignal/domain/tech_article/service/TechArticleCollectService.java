package com.bootsignal.domain.tech_article.service;

import com.bootsignal.domain.tech_article.dto.ParsedRssArticle;
import com.bootsignal.domain.tech_article.dto.TechArticleCollectResponse;
import com.bootsignal.domain.tech_article.dto.TechArticleCollectResponse.SourceCollectResult;
import com.bootsignal.domain.tech_article.entity.ArticleSource;
import com.bootsignal.domain.tech_article.entity.TechArticle;
import com.bootsignal.domain.tech_article.repository.TechArticleRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TechArticleCollectService {

	private final RssFeedParser rssFeedParser;
	private final TechArticleRepository techArticleRepository;

	/* 기술 아티클 소스별 수집 */
	@Transactional
	public TechArticleCollectResponse collect(ArticleSource source) {
		// source 쿼리 파라미터 기반 소스 설정 (미입력 시 전체 소스 수집)
		List<ArticleSource> targets = source == null
			? Arrays.asList(ArticleSource.values())
			: List.of(source);

		// 소스별 수집 결과 조회
		List<SourceCollectResult> results = targets.stream()
			.map(this::collectSource)
			.toList();

		return new TechArticleCollectResponse(results);
	}

 	// 기술 아티클 소스별 수집
	private SourceCollectResult collectSource(ArticleSource source) {
		List<ParsedRssArticle> articles;
		
		// RSS 피드 가져오기 시도
		try {
			articles = rssFeedParser.fetchAndParse(source);
		} catch (IOException e) {
			throw new BootSignalException(ErrorCode.INTERNAL_SERVER_ERROR,
				source + " RSS 피드를 가져오지 못했습니다: " + e.getMessage());
		}

		// 기술 아티클 소스별 수집 결과 조회
		int inserted = 0;
		int updated = 0;
		for (ParsedRssArticle article : articles) {
			var existingOpt = techArticleRepository.findBySourceAndRssGuid(source, article.rssGuid());
			if (existingOpt.isPresent()) { // 기존 아티클 존재 시 업데이트
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
			} else { // 기존 아티클 없으면 생성
				techArticleRepository.save(TechArticle.builder()
					.source(source)
					.title(article.title())
					.summary(article.summary())
					.thumbnailUrl(article.thumbnailUrl())
					.author(article.author())
					.articleUrl(article.articleUrl())
					.publishedAt(article.publishedAt())
					.rssGuid(article.rssGuid())
					.build());
				inserted++;
			}
		}

		return new SourceCollectResult(source, inserted, updated, articles.size());
	}
}
