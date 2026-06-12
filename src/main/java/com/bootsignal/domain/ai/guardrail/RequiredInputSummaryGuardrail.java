package com.bootsignal.domain.ai.guardrail;

import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RequiredInputSummaryGuardrail implements InputGuardrail {

	@Override
	public GuardrailResult validate(AgentExecutionContext context) {
		if (!StringUtils.hasText(context.inputSummary())) {
			return GuardrailResult.fail(ErrorCode.AI_INPUT_INVALID, "AI 실행 입력 요약은 필수입니다.");
		}
		return GuardrailResult.pass();
	}
}
