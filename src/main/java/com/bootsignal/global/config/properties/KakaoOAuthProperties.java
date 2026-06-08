package com.bootsignal.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth.kakao")
public record KakaoOAuthProperties(
	String clientId,
	String jwkSetUri
) {

	public KakaoOAuthProperties {
		jwkSetUri = (jwkSetUri == null || jwkSetUri.isBlank())
			? "https://kauth.kakao.com/.well-known/jwks.json"
			: jwkSetUri;
	}
}
