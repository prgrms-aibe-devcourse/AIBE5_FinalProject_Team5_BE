package com.bootsignal.domain.ai.agent;

import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.domain.ai.harness.AgentExecutionResult;

public interface AiAgent {

	// 기능별 AI 작업 단위이며, Harness가 AgentType 기준으로 선택해 실행한다.
	AgentType type();

	AgentExecutionResult execute(AgentExecutionContext context);
}
