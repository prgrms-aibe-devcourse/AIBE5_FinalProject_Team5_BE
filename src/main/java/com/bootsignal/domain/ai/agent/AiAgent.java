package com.bootsignal.domain.ai.agent;

import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.domain.ai.harness.AgentExecutionResult;

public interface AiAgent {

	AgentType type();

	AgentExecutionResult execute(AgentExecutionContext context);
}
