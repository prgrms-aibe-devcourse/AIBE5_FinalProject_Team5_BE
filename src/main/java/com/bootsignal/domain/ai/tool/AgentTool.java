package com.bootsignal.domain.ai.tool;

import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import java.util.Map;

public interface AgentTool {

	// Agent 내부에서 필요한 조회, 정제, 저장 같은 작은 작업을 분리한다.
	String name();

	Map<String, Object> execute(AgentExecutionContext context);
}
