package com.bootsignal.domain.ai.guardrail;

import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.domain.ai.harness.AgentExecutionResult;

@FunctionalInterface
public interface OutputGuardrail {

	GuardrailResult validate(AgentExecutionContext context, AgentExecutionResult result);

	default String name() {
		return getClass().getSimpleName();
	}
}
