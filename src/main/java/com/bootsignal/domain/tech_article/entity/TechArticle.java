package com.bootsignal.domain.tech_article.entity;

import com.bootsignal.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "tech_article",
	uniqueConstraints = @UniqueConstraint(columnNames = {"source", "rss_guid"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TechArticle extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private ArticleSource source;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String summary;

	@Column(length = 500)
	private String thumbnailUrl;

	@Column(length = 100)
	private String author;

	@Column(nullable = false, length = 500)
	private String articleUrl;

	@Column(nullable = false)
	private LocalDateTime publishedAt;

	@Column(name = "rss_guid", nullable = false, length = 500)
	private String rssGuid;

	@Builder
	private TechArticle(
		ArticleSource source,
		String title,
		String summary,
		String thumbnailUrl,
		String author,
		String articleUrl,
		LocalDateTime publishedAt,
		String rssGuid
	) {
		this.source = source;
		this.title = title;
		this.summary = summary;
		this.thumbnailUrl = thumbnailUrl;
		this.author = author;
		this.articleUrl = articleUrl;
		this.publishedAt = publishedAt;
		this.rssGuid = rssGuid;
	}

	// RSS 피드 내용 업데이트
	public void updateFromRss(
		String title,
		String summary,
		String thumbnailUrl,
		String author,
		String articleUrl,
		LocalDateTime publishedAt
	) {
		this.title = title;
		this.summary = summary;
		this.thumbnailUrl = thumbnailUrl;
		this.author = author;
		this.articleUrl = articleUrl;
		this.publishedAt = publishedAt;
	}
}
