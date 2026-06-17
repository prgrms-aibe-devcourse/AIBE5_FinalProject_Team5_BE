package com.bootsignal.global.config.properties;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 비밀번호 재설정 토큰 유효시간, 프론트 재설정 URL, 메일 발송 옵션을 관리하는 설정입니다.
 */
@ConfigurationProperties(prefix = "app.auth.password-reset")
public record PasswordResetProperties(
	@DefaultValue("1800")
	long validitySeconds,

	@DefaultValue("false")
	boolean responseTokenEnabled,

	@DefaultValue("http://localhost:5173/reset-password?token={token}")
	String resetUrlTemplate,

	@DefaultValue
	Mail mail
) {

	public String buildResetUrl(String rawToken) {
		String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
		return resetUrlTemplate.replace("{token}", encodedToken);
	}

	public record Mail(
		@DefaultValue("false")
		boolean enabled,

		@DefaultValue("no-reply@bootsignal.com")
		String from,

		@DefaultValue("BootSignal 비밀번호 재설정 안내")
		String subject
	) {
	}
}
