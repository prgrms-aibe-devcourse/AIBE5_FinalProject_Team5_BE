package com.bootsignal.domain.ai.review.tool;

import com.bootsignal.domain.ai.exception.AiNonRetryableException;
import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.domain.ai.tool.AgentTool;
import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course.repository.CourseRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CrawledReviewLoadTool implements AgentTool {

	// PR #38의 crawled_review 테이블을 기준으로 과정별 고용24 수강후기를 조회한다.
	private final CourseRepository courseRepository;
	private final EntityManager entityManager;
	private final ReviewSummaryInputNormalizeTool normalizeTool;

	@Override
	public String name() {
		return "crawled-review-load";
	}

	@Override
	@Transactional(readOnly = true)
	public Map<String, Object> execute(AgentExecutionContext context) {
		return Map.of("reviewSummaryInput", load(context));
	}

	@Transactional(readOnly = true)
	public ReviewSummaryInput load(AgentExecutionContext context) {
		Long courseId = normalizeTool.courseId(context);
		int maxReviewCount = normalizeTool.maxReviewCount(context);
		Course course = courseRepository.findById(courseId)
			.orElseThrow(() -> new BootSignalException(ErrorCode.COURSE_NOT_FOUND));
		List<CrawledReviewSnippet> reviews = findReviews(courseId, maxReviewCount);
		if (reviews.isEmpty()) {
			throw new AiNonRetryableException(ErrorCode.AI_INPUT_INVALID, "요약할 고용24 수강후기가 없습니다.");
		}
		return new ReviewSummaryInput(
			course.getId(),
			course.getTitle(),
			reviews.size(),
			averageRating(reviews),
			reviews
		);
	}

	private List<CrawledReviewSnippet> findReviews(Long courseId, int maxReviewCount) {
		Query query = entityManager.createNativeQuery("""
			SELECT id, rating, content
			FROM crawled_review
			WHERE course_id = :courseId
			  AND content IS NOT NULL
			  AND TRIM(content) <> ''
			ORDER BY crawled_at DESC, id DESC
			""");
		query.setParameter("courseId", courseId);
		query.setMaxResults(maxReviewCount);

		@SuppressWarnings("unchecked")
		List<Object[]> rows = query.getResultList();
		return rows.stream()
			.map(this::toSnippet)
			.filter(review -> StringUtils.hasText(review.content()))
			.toList();
	}

	private CrawledReviewSnippet toSnippet(Object[] row) {
		return new CrawledReviewSnippet(
			toLong(row[0]),
			toInteger(row[1]),
			String.valueOf(row[2]).strip()
		);
	}

	private BigDecimal averageRating(List<CrawledReviewSnippet> reviews) {
		List<Integer> ratings = reviews.stream()
			.map(CrawledReviewSnippet::rating)
			.filter(rating -> rating != null && rating > 0)
			.toList();
		if (ratings.isEmpty()) {
			return BigDecimal.ZERO;
		}
		int sum = ratings.stream().mapToInt(Integer::intValue).sum();
		return BigDecimal.valueOf(sum)
			.divide(BigDecimal.valueOf(ratings.size()), 2, RoundingMode.HALF_UP);
	}

	private Long toLong(Object value) {
		return value instanceof Number number ? number.longValue() : null;
	}

	private Integer toInteger(Object value) {
		return value instanceof Number number ? number.intValue() : null;
	}
}
