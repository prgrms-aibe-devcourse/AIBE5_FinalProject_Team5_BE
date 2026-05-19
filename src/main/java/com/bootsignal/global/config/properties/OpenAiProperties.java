package com.bootsignal.global.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.openai")
public record OpenAiProperties(
	String apiKey,
	@NotBlank String model
) {
}

