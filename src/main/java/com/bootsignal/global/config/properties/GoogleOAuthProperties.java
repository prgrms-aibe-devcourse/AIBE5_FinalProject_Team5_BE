package com.bootsignal.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth.google")
public record GoogleOAuthProperties(
	String clientId,
	String clientSecret,
	String jwkSetUri
) {

	public GoogleOAuthProperties {
		clientSecret = clientSecret == null ? "" : clientSecret;
		jwkSetUri = (jwkSetUri == null || jwkSetUri.isBlank())
			? "https://www.googleapis.com/oauth2/v3/certs"
			: jwkSetUri;
	}
}
