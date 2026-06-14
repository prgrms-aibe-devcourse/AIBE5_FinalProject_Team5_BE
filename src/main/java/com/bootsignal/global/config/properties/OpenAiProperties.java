package com.bootsignal.global.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.openai")
public record OpenAiProperties(
	String apiKey,
	@NotBlank String model,
	Integer timeoutMillis,
	Double temperature,
	Integer maxOutputTokens
) {

	private static final int DEFAULT_TIMEOUT_MILLIS = 30_000;

	public OpenAiProperties {
		model = StringUtils.hasText(model) ? model.strip() : model;
		timeoutMillis = timeoutMillis == null ? DEFAULT_TIMEOUT_MILLIS : timeoutMillis;
		if (timeoutMillis < 1) {
			throw new IllegalArgumentException("OpenAI timeoutMillis는 1 이상이어야 합니다.");
		}
		if (temperature != null && (temperature < 0 || temperature > 2)) {
			throw new IllegalArgumentException("OpenAI temperature는 0 이상 2 이하이어야 합니다.");
		}
		if (maxOutputTokens != null && maxOutputTokens < 1) {
			throw new IllegalArgumentException("OpenAI maxOutputTokens는 1 이상이어야 합니다.");
		}
	}
}
