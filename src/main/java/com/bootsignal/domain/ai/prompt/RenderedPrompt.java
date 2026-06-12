package com.bootsignal.domain.ai.prompt;

import com.bootsignal.domain.ai.openai.OpenAiRequest;
import org.springframework.util.StringUtils;

public record RenderedPrompt(
	String name,
	String version,
	String systemPrompt,
	String userPrompt
) {

	public RenderedPrompt {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("렌더링된 프롬프트 이름은 필수입니다.");
		}
		if (!StringUtils.hasText(version)) {
			throw new IllegalArgumentException("렌더링된 프롬프트 버전은 필수입니다.");
		}
		if (!StringUtils.hasText(userPrompt)) {
			throw new IllegalArgumentException("렌더링된 사용자 프롬프트는 필수입니다.");
		}

		name = name.strip();
		version = version.strip();
		systemPrompt = StringUtils.hasText(systemPrompt) ? systemPrompt.strip() : "";
		userPrompt = userPrompt.strip();
	}

	public String promptVersion() {
		return name + ":" + version;
	}

	public OpenAiRequest toOpenAiRequest(String model) {
		return new OpenAiRequest(model, systemPrompt, userPrompt, promptVersion(), null, null);
	}
}
