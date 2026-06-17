package com.bootsignal.domain.tech_article.dto;

import com.bootsignal.domain.tech_article.entity.ArticleSource;
import com.bootsignal.domain.tech_article.entity.TechArticle;
import java.time.LocalDateTime;

public record TechArticleResponse(
	Long id,
	ArticleSource source,
	String title,
	String summary,
	String thumbnailUrl,
	String author,
	String articleUrl,
	LocalDateTime publishedAt,
	LocalDateTime updatedAt
) {

	public static TechArticleResponse from(TechArticle article) {
		return new TechArticleResponse(
			article.getId(),
			article.getSource(),
			article.getTitle(),
			article.getSummary(),
			article.getThumbnailUrl(),
			article.getAuthor(),
			article.getArticleUrl(),
			article.getPublishedAt(),
			article.getUpdatedAt()
		);
	}
}
