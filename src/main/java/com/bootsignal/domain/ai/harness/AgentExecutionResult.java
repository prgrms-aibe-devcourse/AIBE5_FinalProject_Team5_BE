package com.bootsignal.domain.ai.harness;

import com.bootsignal.domain.ai.agent.AgentType;
import com.bootsignal.domain.ai.log.AgentExecutionStatus;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.util.StringUtils;

public record AgentExecutionResult(
	UUID executionId,
	AgentType agentType,
	AgentExecutionStatus status,
	String outputSummary,
	Map<String, Object> output,
	String errorMessage
) {

	public AgentExecutionResult {
		// Service는 Agent 내부 구현을 몰라도 공통 결과 모델만 다룬다.
		if (executionId == null) {
			throw new IllegalArgumentException("AI 실행 ID는 필수입니다.");
		}
		if (agentType == null) {
			throw new IllegalArgumentException("AI Agent 타입은 필수입니다.");
		}
		if (status == null) {
			throw new IllegalArgumentException("AI 실행 상태는 필수입니다.");
		}

		outputSummary = normalize(outputSummary);
		output = immutableOutput(output);
		errorMessage = normalize(errorMessage);
	}

	public static AgentExecutionResult success(
		AgentExecutionContext context,
		String outputSummary,
		Map<String, Object> output
	) {
		return new AgentExecutionResult(
			context.executionId(),
			context.agentType(),
			AgentExecutionStatus.SUCCESS,
			outputSummary,
			output,
			null
		);
	}

	public static AgentExecutionResult failure(AgentExecutionContext context, String errorMessage) {
		return new AgentExecutionResult(
			context.executionId(),
			context.agentType(),
			AgentExecutionStatus.FAILED,
			null,
			Map.of(),
			errorMessage
		);
	}

	public boolean isSuccess() {
		return status == AgentExecutionStatus.SUCCESS;
	}

	private static String normalize(String value) {
		return StringUtils.hasText(value) ? value.strip() : "";
	}

	private static Map<String, Object> immutableOutput(Map<String, Object> output) {
		if (output == null || output.isEmpty()) {
			return Map.of();
		}
		return Collections.unmodifiableMap(new LinkedHashMap<>(output));
	}
}
