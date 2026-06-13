package com.bootsignal.domain.ai.review.service;

import com.bootsignal.domain.ai.agent.AgentType;
import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.domain.ai.harness.AgentExecutionResult;
import com.bootsignal.domain.ai.harness.AgentHarness;
import com.bootsignal.domain.ai.review.dto.ReviewSummaryContent;
import com.bootsignal.domain.ai.review.dto.ReviewSummaryCreateRequest;
import com.bootsignal.domain.ai.review.dto.ReviewSummaryResponse;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.SecurityUtil;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewSummaryService {

	// 인증된 사용자의 요청을 AI 실행 컨텍스트로 변환해 리뷰 요약 Agent에 위임한다.
	private final AgentHarness agentHarness;
	private final UserRepository userRepository;

	public ReviewSummaryResponse createSummary(ReviewSummaryCreateRequest request) {
		User user = getAuthenticatedUser();
		AgentExecutionResult result = agentHarness.execute(AgentExecutionContext.of(
			AgentType.REVIEW_SUMMARY,
			user.getId(),
			toInputSummary(request),
			toInput(request)
		));

		if (!result.isSuccess()) {
			throw new BootSignalException(ErrorCode.AI_EXECUTION_FAILED, result.errorMessage());
		}

		Object summary = result.output().get("summary");
		if (!(summary instanceof ReviewSummaryContent content)) {
			throw new BootSignalException(ErrorCode.AI_OUTPUT_INVALID, "수강후기 요약 결과를 찾을 수 없습니다.");
		}
		return ReviewSummaryResponse.from(
			result.executionId(),
			asLong(result.output().get("courseId")),
			asString(result.output().get("courseTitle")),
			asInt(result.output().get("reviewCount")),
			asBigDecimal(result.output().get("averageRating")),
			content
		);
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
