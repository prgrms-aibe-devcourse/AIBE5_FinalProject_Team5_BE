package com.bootsignal.domain.ai.openai;

import org.springframework.util.StringUtils;

public record OpenAiResponse(
	String model,
	String content,
	Integer promptTokens,
	Integer completionTokens
) {

	public OpenAiResponse {
		if (!StringUtils.hasText(content)) {
			throw new IllegalArgumentException("OpenAI 응답 내용은 필수입니다.");
		}

		model = StringUtils.hasText(model) ? model.strip() : "";
		content = content.strip();
	}
}
