package com.bootsignal.domain.ai.log;

import org.springframework.util.StringUtils;

public record AgentExecutionMetadata(
	String model,
	String promptVersion,
	Integer promptTokens,
	Integer completionTokens,
	Integer totalTokens,
	Integer reasoningTokens,
	Double temperature
) {

	private static final AgentExecutionMetadata EMPTY = new AgentExecutionMetadata(
		"",
		"",
		null,
		null,
		null,
		null,
		null
	);

	public AgentExecutionMetadata {
		// 원문 프롬프트 대신 식별 가능한 버전과 토큰 사용량만 실행 로그에 남긴다.
		model = StringUtils.hasText(model) ? model.strip() : "";
		promptVersion = StringUtils.hasText(promptVersion) ? promptVersion.strip() : "";
		validateToken("promptTokens", promptTokens);
		validateToken("completionTokens", completionTokens);
		validateToken("totalTokens", totalTokens);
		validateToken("reasoningTokens", reasoningTokens);
		if (temperature != null && (temperature < 0 || temperature > 2)) {
			throw new IllegalArgumentException("AI temperature는 0 이상 2 이하이어야 합니다.");
		}
	}

	public static AgentExecutionMetadata empty() {
		return EMPTY;
	}

	public boolean hasModel() {
		return StringUtils.hasText(model);
	}

	public boolean hasPromptVersion() {
		return StringUtils.hasText(promptVersion);
	}

	public boolean hasTokenUsage() {
		return promptTokens != null
			|| completionTokens != null
			|| totalTokens != null
			|| reasoningTokens != null;
	}

	private static void validateToken(String name, Integer value) {
		if (value != null && value < 0) {
			throw new IllegalArgumentException(name + "는 0 이상이어야 합니다.");
		}
	}
}
