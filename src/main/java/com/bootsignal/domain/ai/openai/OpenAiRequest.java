package com.bootsignal.domain.ai.openai;

import org.springframework.util.StringUtils;

public record OpenAiRequest(
	String model,
	String systemPrompt,
	String userPrompt
) {

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
	}
}
