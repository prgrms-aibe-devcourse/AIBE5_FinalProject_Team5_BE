package com.bootsignal.global.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 발급자, 비밀키, access/refresh token 유효 시간을 담는 설정 값입니다.
 */
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
	@NotBlank String issuer,
	@NotBlank @Size(min = 32) String secret,
	@Min(60) long accessTokenValiditySeconds,
	@Min(60) long refreshTokenValiditySeconds
) {
}
