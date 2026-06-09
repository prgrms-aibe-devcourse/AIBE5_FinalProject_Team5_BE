package com.bootsignal.domain.ai.harness;

import com.bootsignal.domain.ai.agent.AgentType;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.util.StringUtils;

public record AgentExecutionContext(
	UUID executionId,
	AgentType agentType,
	Long userId,
	String inputSummary,
	Map<String, Object> input
) {

	public AgentExecutionContext {
		if (executionId == null) {
			throw new IllegalArgumentException("AI 실행 ID는 필수입니다.");
		}
		if (agentType == null) {
			throw new IllegalArgumentException("AI Agent 타입은 필수입니다.");
		}

		inputSummary = normalize(inputSummary);
		input = immutableInput(input);
	}

	public static AgentExecutionContext of(AgentType agentType, Long userId, String inputSummary) {
		return new AgentExecutionContext(UUID.randomUUID(), agentType, userId, inputSummary, Map.of());
	}

	public static AgentExecutionContext of(
		AgentType agentType,
		Long userId,
		String inputSummary,
		Map<String, Object> input
	) {
		return new AgentExecutionContext(UUID.randomUUID(), agentType, userId, inputSummary, input);
	}

	private static String normalize(String value) {
		return StringUtils.hasText(value) ? value.strip() : "";
	}

	private static Map<String, Object> immutableInput(Map<String, Object> input) {
		if (input == null || input.isEmpty()) {
			return Map.of();
		}
		return Collections.unmodifiableMap(new LinkedHashMap<>(input));
	}
}
