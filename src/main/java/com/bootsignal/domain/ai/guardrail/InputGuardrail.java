package com.bootsignal.domain.ai.guardrail;

import com.bootsignal.domain.ai.harness.AgentExecutionContext;

@FunctionalInterface
public interface InputGuardrail {

	GuardrailResult validate(AgentExecutionContext context);

	default String name() {
		return getClass().getSimpleName();
	}
}
