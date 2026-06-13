package com.bootsignal.domain.ai.review.tool;

import com.bootsignal.domain.ai.exception.AiNonRetryableException;
import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.domain.ai.tool.AgentTool;
import com.bootsignal.global.exception.ErrorCode;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReviewSummaryInputNormalizeTool implements AgentTool {

	// 수강후기 요약 Agent가 사용할 과정 ID와 조회 개수를 안전한 숫자 값으로 정리한다.
	private static final int DEFAULT_MAX_REVIEW_COUNT = 50;
	private static final int MAX_REVIEW_COUNT = 200;

	@Override
	public String name() {
		return "review-summary-input-normalize";
	}

	@Override
	public Map<String, Object> execute(AgentExecutionContext context) {
		return Map.of(
			"courseId", courseId(context),
			"maxReviewCount", maxReviewCount(context)
		);
	}

	public Long courseId(AgentExecutionContext context) {
		Object value = context.input().get("courseId");
		if (!(value instanceof Number number) || number.longValue() < 1) {
			throw invalidInput("요약할 과정 ID는 필수입니다.");
		}
		return number.longValue();
	}

	public int maxReviewCount(AgentExecutionContext context) {
		Object value = context.input().get("maxReviewCount");
		if (value == null) {
			return DEFAULT_MAX_REVIEW_COUNT;
		}
		if (!(value instanceof Number number)) {
			throw invalidInput("수강후기 조회 개수 형식이 올바르지 않습니다.");
		}
		int count = number.intValue();
		if (count < 1 || count > MAX_REVIEW_COUNT) {
			throw invalidInput("수강후기 조회 개수는 1개 이상 200개 이하이어야 합니다.");
		}
		return count;
	}

	private AiNonRetryableException invalidInput(String message) {
		return new AiNonRetryableException(ErrorCode.AI_INPUT_INVALID, message);
	}
}
