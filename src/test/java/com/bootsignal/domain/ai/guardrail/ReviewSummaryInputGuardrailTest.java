package com.bootsignal.domain.ai.guardrail;

import static org.assertj.core.api.Assertions.assertThat;

import com.bootsignal.domain.ai.agent.AgentType;
import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.global.exception.ErrorCode;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReviewSummaryInputGuardrailTest {

	// 리뷰 요약 전용 Guardrail이 과정 ID와 조회 개수만 검증하는지 확인한다.
	private final ReviewSummaryInputGuardrail guardrail = new ReviewSummaryInputGuardrail();

	@Test
	void validatePassesForOtherAgentTypes() {
		AgentExecutionContext context = AgentExecutionContext.of(
			AgentType.PORTFOLIO_DRAFT,
			1L,
			"포트폴리오 초안 생성 요청",
			Map.of()
		);

		assertThat(guardrail.validate(context).valid()).isTrue();
	}

	@Test
	void validateFailsWhenCourseIdIsMissing() {
		AgentExecutionContext context = AgentExecutionContext.of(
			AgentType.REVIEW_SUMMARY,
			1L,
			"고용24 수강후기 요약 요청",
			Map.of("maxReviewCount", 50)
		);

		GuardrailResult result = guardrail.validate(context);

		assertThat(result.valid()).isFalse();
		assertThat(result.errorCode()).isEqualTo(ErrorCode.AI_INPUT_INVALID);
		assertThat(result.message()).isEqualTo("요약할 과정 ID는 필수입니다.");
	}
}
