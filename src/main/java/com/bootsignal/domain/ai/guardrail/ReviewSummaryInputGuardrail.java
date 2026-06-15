package com.bootsignal.domain.ai.guardrail;

import com.bootsignal.domain.ai.agent.AgentType;
import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class ReviewSummaryInputGuardrail implements InputGuardrail {

	// 리뷰 요약 Agent는 과정 ID와 유효한 조회 개수가 있어야 실행한다.
	private static final int MAX_REVIEW_COUNT = 200;

	@Override
	public GuardrailResult validate(AgentExecutionContext context) {
		if (context.agentType() != AgentType.REVIEW_SUMMARY) {
			return GuardrailResult.pass();
		}
		if (!positiveNumber(context.input().get("courseId"))) {
			return GuardrailResult.fail(ErrorCode.AI_INPUT_INVALID, "요약할 과정 ID는 필수입니다.");
		}
		Object maxReviewCount = context.input().get("maxReviewCount");
		if (maxReviewCount != null && !validMaxReviewCount(maxReviewCount)) {
			return GuardrailResult.fail(ErrorCode.AI_INPUT_INVALID, "수강후기 조회 개수는 1개 이상 200개 이하이어야 합니다.");
		}
		return GuardrailResult.pass();
	}

	private boolean positiveNumber(Object value) {
		return value instanceof Number number && number.longValue() > 0;
	}

	private boolean validMaxReviewCount(Object value) {
		return value instanceof Number number
			&& number.intValue() >= 1
			&& number.intValue() <= MAX_REVIEW_COUNT;
	}
}
