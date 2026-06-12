package com.bootsignal.domain.ai.openai;

import com.bootsignal.domain.ai.log.AgentExecutionMetadata;
import org.springframework.util.StringUtils;

public record OpenAiResponse(
	String model,
	String content,
	Integer promptTokens,
	Integer completionTokens,
	Integer totalTokens,
	Integer reasoningTokens
) {

	public OpenAiResponse(String model, String content, Integer promptTokens, Integer completionTokens) {
		this(model, content, promptTokens, completionTokens, sumTokens(promptTokens, completionTokens), null);
	}

	public OpenAiResponse {
		if (!StringUtils.hasText(content)) {
			throw new IllegalArgumentException("OpenAI 응답 내용은 필수입니다.");
		}

		model = StringUtils.hasText(model) ? model.strip() : "";
		content = content.strip();
		validateToken("promptTokens", promptTokens);
		validateToken("completionTokens", completionTokens);
		validateToken("totalTokens", totalTokens);
		validateToken("reasoningTokens", reasoningTokens);
	}

	public AgentExecutionMetadata toExecutionMetadata(String promptVersion, Double temperature) {
		return new AgentExecutionMetadata(
			model,
			promptVersion,
			promptTokens,
			completionTokens,
			totalTokens,
			reasoningTokens,
			temperature
		);
	}

	private static Integer sumTokens(Integer promptTokens, Integer completionTokens) {
		if (promptTokens == null || completionTokens == null) {
			return null;
		}
		return promptTokens + completionTokens;
	}

	private static void validateToken(String name, Integer value) {
		if (value != null && value < 0) {
			throw new IllegalArgumentException(name + "는 0 이상이어야 합니다.");
		}
	}
}
