package com.bootsignal.domain.ai.review.service;

import com.bootsignal.domain.ai.agent.AgentType;
import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.domain.ai.harness.AgentExecutionResult;
import com.bootsignal.domain.ai.harness.AgentHarness;
import com.bootsignal.domain.ai.review.dto.ReviewSummaryContent;
import com.bootsignal.domain.ai.review.dto.ReviewSummaryCreateRequest;
import com.bootsignal.domain.ai.review.dto.ReviewSummaryResponse;
import com.bootsignal.domain.ai.review.entity.ReviewSummaryCache;
import com.bootsignal.domain.ai.review.repository.ReviewSummaryCacheRepository;
import com.bootsignal.domain.crawled_review.repository.CrawledReviewRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.SecurityUtil;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewSummaryService {

	private final AgentHarness agentHarness;
	private final UserRepository userRepository;
	private final ReviewSummaryCacheRepository cacheRepository;
	private final CrawledReviewRepository crawledReviewRepository;

	@Transactional
	public ReviewSummaryResponse createSummary(ReviewSummaryCreateRequest request) {
		getAuthenticatedUser();
		Long courseId = request.courseId();

		Object[] snapshot = resolveSnapshot(courseId);
		int currentCount = toInt(snapshot[0]);
		Instant currentLatestCrawledAt = (Instant) snapshot[1];

		Optional<ReviewSummaryCache> cached = cacheRepository.findByCourseId(courseId);

		if (cached.isPresent()) {
			ReviewSummaryCache cache = cached.get();
			if (!cache.isStale(currentCount, currentLatestCrawledAt)) {
				return toCachedResponse(cache);
			}
			return regenerateAndUpdate(request, cache);
		}

		return generateAndSave(request, currentCount, currentLatestCrawledAt);
	}

	private ReviewSummaryResponse generateAndSave(ReviewSummaryCreateRequest request,
		int reviewCount, Instant latestCrawledAt) {
		AgentExecutionResult result = executeAgent(request);
		ReviewSummaryContent content = extractContent(result);

		ReviewSummaryCache cache = ReviewSummaryCache.builder()
			.courseId(asLong(result.output().get("courseId")))
			.executionId(result.executionId().toString())
			.reviewCount(reviewCount)
			.latestCrawledAt(latestCrawledAt)
			.averageRating(asBigDecimal(result.output().get("averageRating")))
			.courseTitle(asString(result.output().get("courseTitle")))
			.summary(content.summary())
			.strengths(content.strengths())
			.weaknesses(content.weaknesses())
			.recommendedFor(content.recommendedFor())
			.keywords(content.keywords())
			.build();
		cacheRepository.save(cache);

		return toResponse(result, content);
	}

	private ReviewSummaryResponse regenerateAndUpdate(ReviewSummaryCreateRequest request,
		ReviewSummaryCache cache) {
		AgentExecutionResult result = executeAgent(request);
		ReviewSummaryContent content = extractContent(result);

		Object[] snapshot = resolveSnapshot(request.courseId());
		cache.update(
			result.executionId().toString(),
			toInt(snapshot[0]),
			(Instant) snapshot[1],
			asBigDecimal(result.output().get("averageRating")),
			asString(result.output().get("courseTitle")),
			content
		);

		return toResponse(result, content);
	}

	private ReviewSummaryResponse toCachedResponse(ReviewSummaryCache cache) {
		return ReviewSummaryResponse.from(
			UUID.fromString(cache.getExecutionId()),
			cache.getCourseId(),
			cache.getCourseTitle(),
			cache.getReviewCount(),
			cache.getAverageRating(),
			cache.toContent()
		);
	}

	private AgentExecutionResult executeAgent(ReviewSummaryCreateRequest request) {
		AgentExecutionResult result = agentHarness.execute(AgentExecutionContext.of(
			AgentType.REVIEW_SUMMARY,
			getAuthenticatedUser().getId(),
			toInputSummary(request),
			toInput(request)
		));
		if (!result.isSuccess()) {
			throw new BootSignalException(ErrorCode.AI_EXECUTION_FAILED, result.errorMessage());
		}
		return result;
	}

	private ReviewSummaryContent extractContent(AgentExecutionResult result) {
		Object summary = result.output().get("summary");
		if (!(summary instanceof ReviewSummaryContent content)) {
			throw new BootSignalException(ErrorCode.AI_OUTPUT_INVALID, "수강후기 요약 결과를 찾을 수 없습니다.");
		}
		return content;
	}

	private ReviewSummaryResponse toResponse(AgentExecutionResult result, ReviewSummaryContent content) {
		return ReviewSummaryResponse.from(
			result.executionId(),
			asLong(result.output().get("courseId")),
			asString(result.output().get("courseTitle")),
			asInt(result.output().get("reviewCount")),
			asBigDecimal(result.output().get("averageRating")),
			content
		);
	}

	private Object[] resolveSnapshot(Long courseId) {
		return crawledReviewRepository.findReviewSnapshotByCourseId(courseId)
			.filter(row -> row[0] != null && toInt(row[0]) > 0)
			.orElseThrow(() -> new BootSignalException(ErrorCode.AI_INPUT_INVALID, "요약할 고용24 수강후기가 없습니다."));
	}

	private Map<String, Object> toInput(ReviewSummaryCreateRequest request) {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("courseId", request.courseId());
		input.put("maxReviewCount", request.resolvedMaxReviewCount());
		return input;
	}

	private String toInputSummary(ReviewSummaryCreateRequest request) {
		return "고용24 수강후기 요약 요청 - 과정 ID: "
			+ request.courseId()
			+ ", 최대 후기 수: "
			+ request.resolvedMaxReviewCount();
	}

	private User getAuthenticatedUser() {
		String email = SecurityUtil.getCurrentUserEmail();
		return userRepository.findByEmail(email)
			.filter(user -> !user.isDeleted())
			.orElseThrow(() -> new BootSignalException(ErrorCode.UNAUTHORIZED));
	}

	private Long asLong(Object value) {
		return value instanceof Number number ? number.longValue() : null;
	}

	private int asInt(Object value) {
		return value instanceof Number number ? number.intValue() : 0;
	}

	private int toInt(Object value) {
		return value instanceof Number number ? number.intValue() : 0;
	}

	private String asString(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	private BigDecimal asBigDecimal(Object value) {
		if (value instanceof BigDecimal bigDecimal) {
			return bigDecimal;
		}
		if (value instanceof Number number) {
			return BigDecimal.valueOf(number.doubleValue());
		}
		return BigDecimal.ZERO;
	}
}
