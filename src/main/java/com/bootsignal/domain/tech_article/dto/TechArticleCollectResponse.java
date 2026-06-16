package com.bootsignal.domain.tech_article.dto;

import com.bootsignal.domain.tech_article.entity.ArticleSource;
import java.util.List;

/* RSS 수집 결과 */
public record TechArticleCollectResponse(
	List<SourceCollectResult> results
) {

	public record SourceCollectResult(
		ArticleSource source, // 소스 출처
		int inserted, // 삽입된 아티클 수
		int updated, // 업데이트된 아티클 수
		int totalFetched, // 총 가져온 아티클 수
		int filteredOut, // 필터링된 아티클 수
		int keywordMatched // 키워드 통과 건수 (저장 limit 적용 전)
	) {
	}
}
