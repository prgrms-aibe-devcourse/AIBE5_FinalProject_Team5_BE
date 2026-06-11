package com.bootsignal.domain.ai.openai;

import org.springframework.util.StringUtils;

public record OpenAiRequest(
	String model,
	String systemPrompt,
	String userPrompt,
	String promptVersion,
	Double temperature,
	Integer maxOutputTokens
) {

	public OpenAiRequest(String model, String systemPrompt, String userPrompt) {
		this(model, systemPrompt, userPrompt, null, null, null);
	}

	public OpenAiRequest {
		if (!StringUtils.hasText(model)) {
			throw new IllegalArgumentException("OpenAI 모델은 필수입니다.");
		}
		if (!StringUtils.hasText(userPrompt)) {
			throw new IllegalArgumentException("OpenAI 사용자 프롬프트는 필수입니다.");
		}

		model = model.strip();
		systemPrompt = StringUtils.hasText(systemPrompt) ? systemPrompt.strip() : "";
		userPrompt = userPrompt.strip();
		promptVersion = StringUtils.hasText(promptVersion) ? promptVersion.strip() : "";
		if (temperature != null && (temperature < 0 || temperature > 2)) {
			throw new IllegalArgumentException("OpenAI temperature는 0 이상 2 이하이어야 합니다.");
		}
		if (maxOutputTokens != null && maxOutputTokens < 1) {
			throw new IllegalArgumentException("OpenAI maxOutputTokens는 1 이상이어야 합니다.");
		}
	}
}
