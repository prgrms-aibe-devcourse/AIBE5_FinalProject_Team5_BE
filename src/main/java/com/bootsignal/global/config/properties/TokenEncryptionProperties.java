package com.bootsignal.global.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.token-encryption")
public record TokenEncryptionProperties(
	@NotBlank String secret
) {
}
