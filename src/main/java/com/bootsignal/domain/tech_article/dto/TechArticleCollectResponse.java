package com.bootsignal.domain.tech_article.dto;

import com.bootsignal.domain.tech_article.entity.ArticleSource;
import java.util.List;

/* RSS 수집 결과 */
public record TechArticleCollectResponse(
	List<SourceCollectResult> results
) {

	public record SourceCollectResult(
		ArticleSource source, // 소스 종류
		int inserted, // 삽입된 아티클 수
		int updated, // 업데이트된 아티클 수
		int totalFetched // 총 가져온 아티클 수
	) {
	}
}
