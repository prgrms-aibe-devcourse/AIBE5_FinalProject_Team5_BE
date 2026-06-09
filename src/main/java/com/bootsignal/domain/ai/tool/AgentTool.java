package com.bootsignal.domain.ai.tool;

import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import java.util.Map;

public interface AgentTool {

	String name();

	Map<String, Object> execute(AgentExecutionContext context);
}
