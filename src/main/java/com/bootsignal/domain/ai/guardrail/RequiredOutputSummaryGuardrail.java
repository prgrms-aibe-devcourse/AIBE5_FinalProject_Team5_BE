package com.bootsignal.domain.ai.guardrail;

import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.domain.ai.harness.AgentExecutionResult;
import com.bootsignal.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RequiredOutputSummaryGuardrail implements OutputGuardrail {

	@Override
	public GuardrailResult validate(AgentExecutionContext context, AgentExecutionResult result) {
		if (!StringUtils.hasText(result.outputSummary())) {
			return GuardrailResult.retryableFail(ErrorCode.AI_OUTPUT_INVALID, "AI 실행 출력 요약은 필수입니다.");
		}
		return GuardrailResult.pass();
	}
}
