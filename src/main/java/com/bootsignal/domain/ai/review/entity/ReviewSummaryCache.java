package com.bootsignal.domain.ai.review.entity;

import com.bootsignal.domain.ai.review.dto.ReviewSummaryContent;
import com.bootsignal.global.converter.StringListConverter;
import com.bootsignal.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "review_summary_cache")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewSummaryCache extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "course_id", nullable = false, unique = true)
	private Long courseId;

	@Column(name = "execution_id", nullable = false, length = 36)
	private String executionId;

	@Column(name = "review_count", nullable = false)
	private int reviewCount;

	@Column(name = "latest_crawled_at", nullable = false)
	private Instant latestCrawledAt;

	@Column(name = "average_rating", precision = 3, scale = 2)
	private BigDecimal averageRating;

	@Column(name = "course_title", nullable = false, length = 500)
	private String courseTitle;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String summary;

	@Convert(converter = StringListConverter.class)
	@Column(columnDefinition = "TEXT")
	private List<String> strengths;

	@Convert(converter = StringListConverter.class)
	@Column(columnDefinition = "TEXT")
	private List<String> weaknesses;

	@Convert(converter = StringListConverter.class)
	@Column(name = "recommended_for", columnDefinition = "TEXT")
	private List<String> recommendedFor;

	@Convert(converter = StringListConverter.class)
	@Column(columnDefinition = "TEXT")
	private List<String> keywords;

	@Builder
	private ReviewSummaryCache(Long courseId, String executionId, int reviewCount,
		Instant latestCrawledAt, BigDecimal averageRating, String courseTitle,
		String summary, List<String> strengths, List<String> weaknesses,
		List<String> recommendedFor, List<String> keywords) {
		this.courseId = courseId;
		this.executionId = executionId;
		this.reviewCount = reviewCount;
		this.latestCrawledAt = latestCrawledAt;
		this.averageRating = averageRating;
		this.courseTitle = courseTitle;
		this.summary = summary;
		this.strengths = strengths;
		this.weaknesses = weaknesses;
		this.recommendedFor = recommendedFor;
		this.keywords = keywords;
	}

	public void update(String executionId, int reviewCount, Instant latestCrawledAt,
		BigDecimal averageRating, String courseTitle, ReviewSummaryContent content) {
		this.executionId = executionId;
		this.reviewCount = reviewCount;
		this.latestCrawledAt = latestCrawledAt;
		this.averageRating = averageRating;
		this.courseTitle = courseTitle;
		this.summary = content.summary();
		this.strengths = content.strengths();
		this.weaknesses = content.weaknesses();
		this.recommendedFor = content.recommendedFor();
		this.keywords = content.keywords();
	}

	public ReviewSummaryContent toContent() {
		return new ReviewSummaryContent(summary, strengths, weaknesses, recommendedFor, keywords);
	}

	public boolean isStale(int currentReviewCount, Instant currentLatestCrawledAt) {
		return this.reviewCount != currentReviewCount
			|| !this.latestCrawledAt.equals(currentLatestCrawledAt);
	}
}
