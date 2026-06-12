package com.bootsignal.domain.ai.guardrail;

import static org.assertj.core.api.Assertions.assertThat;

import com.bootsignal.domain.ai.agent.AgentType;
import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PortfolioDraftInputGuardrailTest {

	// 포트폴리오 전용 Guardrail이 다른 Agent에는 영향을 주지 않는지 함께 검증한다.
	private final PortfolioDraftInputGuardrail guardrail = new PortfolioDraftInputGuardrail();

	@Test
	void validatePassesForOtherAgentTypes() {
		AgentExecutionContext context = AgentExecutionContext.of(
			AgentType.REVIEW_SUMMARY,
			1L,
			"리뷰 요약 요청",
			Map.of()
		);

		assertThat(guardrail.validate(context).valid()).isTrue();
	}

	@Test
	void validateFailsWhenPortfolioTargetJobIsMissing() {
		AgentExecutionContext context = AgentExecutionContext.of(
			AgentType.PORTFOLIO_DRAFT,
			1L,
			"포트폴리오 초안 생성 요청",
			Map.of(
				"skills", List.of("Java"),
				"projects", List.of("프로젝트")
			)
		);

		GuardrailResult result = guardrail.validate(context);

		assertThat(result.valid()).isFalse();
		assertThat(result.errorCode()).isEqualTo(ErrorCode.AI_INPUT_INVALID);
		assertThat(result.message()).isEqualTo("포트폴리오 목표 직무는 필수입니다.");
	}
}
