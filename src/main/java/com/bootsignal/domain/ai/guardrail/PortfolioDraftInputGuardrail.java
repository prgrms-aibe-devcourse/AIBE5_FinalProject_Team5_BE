package com.bootsignal.domain.ai.guardrail;

import com.bootsignal.domain.ai.agent.AgentType;
import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.global.exception.ErrorCode;
import java.util.Collection;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PortfolioDraftInputGuardrail implements InputGuardrail {

	// 포트폴리오 Agent는 목표 직무, 기술 스택, 프로젝트 경험이 있어야 실행한다.
	private static final int MAX_SKILL_COUNT = 20;
	private static final int MAX_PROJECT_COUNT = 10;

	@Override
	public GuardrailResult validate(AgentExecutionContext context) {
		if (context.agentType() != AgentType.PORTFOLIO_DRAFT) {
			return GuardrailResult.pass();
		}

		if (!hasText(context.input().get("targetJob"))) {
			return GuardrailResult.fail(ErrorCode.AI_INPUT_INVALID, "포트폴리오 목표 직무는 필수입니다.");
		}
		GuardrailResult skillResult = validateRequiredCollection(
			context.input().get("skills"),
			MAX_SKILL_COUNT,
			"포트폴리오 기술 스택은 1개 이상 입력해야 합니다.",
			"포트폴리오 기술 스택은 20개 이하로 입력해야 합니다."
		);
		if (!skillResult.valid()) {
			return skillResult;
		}
		return validateRequiredCollection(
			context.input().get("projects"),
			MAX_PROJECT_COUNT,
			"포트폴리오 프로젝트 경험은 1개 이상 입력해야 합니다.",
			"포트폴리오 프로젝트 경험은 10개 이하로 입력해야 합니다."
		);
	}

	private GuardrailResult validateRequiredCollection(
		Object value,
		int maxSize,
		String emptyMessage,
		String maxSizeMessage
	) {
		if (!(value instanceof Collection<?> collection) || collection.isEmpty()) {
			return GuardrailResult.fail(ErrorCode.AI_INPUT_INVALID, emptyMessage);
		}
		if (collection.size() > maxSize) {
			return GuardrailResult.fail(ErrorCode.AI_INPUT_INVALID, maxSizeMessage);
		}
		return GuardrailResult.pass();
	}

	private boolean hasText(Object value) {
		return value instanceof String text && StringUtils.hasText(text);
	}
}
