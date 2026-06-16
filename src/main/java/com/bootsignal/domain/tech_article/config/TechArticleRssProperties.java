package com.bootsignal.domain.tech_article.config;

import com.bootsignal.domain.tech_article.entity.ArticleSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.tech-article.rss")
public record TechArticleRssProperties(
	@NotBlank String yozmFeedUrl,
	@NotBlank String kakaoTechFeedUrl,
	@Positive int collectLimit,
	@Positive int timeoutMillis,
	@NotBlank String userAgent
) {
	// 소스별 RSS 피드 URL 반환
	public String feedUrlOf(ArticleSource source) {
		return switch (source) {
			case YOZM -> yozmFeedUrl;
			case KAKAO_TECH -> kakaoTechFeedUrl;
		};
	}
}
