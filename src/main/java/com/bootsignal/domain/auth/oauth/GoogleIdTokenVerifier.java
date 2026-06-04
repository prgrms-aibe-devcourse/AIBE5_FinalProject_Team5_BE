package com.bootsignal.domain.auth.oauth;

import com.bootsignal.global.config.properties.GoogleOAuthProperties;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.EmailFormatValidator;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GoogleIdTokenVerifier implements GoogleTokenVerifier {

	private static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

	private final GoogleOAuthProperties properties;
	private final JwtDecoder jwtDecoder;

	@Autowired
	public GoogleIdTokenVerifier(GoogleOAuthProperties properties) {
		this(properties, NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build());
	}

	GoogleIdTokenVerifier(GoogleOAuthProperties properties, JwtDecoder jwtDecoder) {
		this.properties = properties;
		this.jwtDecoder = jwtDecoder;
	}

	@Override
	public GoogleUserInfo verify(String idToken) {
		String clientId = resolveClientId();
		Jwt jwt = decode(idToken);

		validateIssuer(jwt);
		validateAudience(jwt, clientId);
		validateEmailVerified(jwt);

		String email = EmailFormatValidator.normalize(jwt.getClaimAsString("email"));
		if (!EmailFormatValidator.isValid(email)) {
			throw invalidToken("Google ID Token의 이메일 정보가 올바르지 않습니다.");
		}

		String subject = jwt.getSubject();
		if (!StringUtils.hasText(subject)) {
			throw invalidToken("Google ID Token의 사용자 식별자가 없습니다.");
		}

		return new GoogleUserInfo(
			subject,
			email,
			resolveName(jwt, email),
			jwt.getClaimAsString("picture")
		);
	}

	private Jwt decode(String idToken) {
		try {
			return jwtDecoder.decode(idToken);
		} catch (JwtException | IllegalArgumentException exception) {
			throw invalidToken("유효하지 않은 Google ID Token입니다.");
		}
	}

	private void validateIssuer(Jwt jwt) {
		String issuer = jwt.getClaimAsString("iss");
		if (!GOOGLE_ISSUERS.contains(issuer)) {
			throw invalidToken("Google ID Token의 발급자가 올바르지 않습니다.");
		}
	}

	private void validateAudience(Jwt jwt, String clientId) {
		if (!jwt.getAudience().contains(clientId)) {
			throw invalidToken("Google ID Token의 대상자가 올바르지 않습니다.");
		}
	}

	private void validateEmailVerified(Jwt jwt) {
		Object emailVerified = jwt.getClaims().get("email_verified");
		boolean verified = Boolean.TRUE.equals(emailVerified)
			|| "true".equalsIgnoreCase(String.valueOf(emailVerified));
		if (!verified) {
			throw invalidToken("Google 이메일 인증이 완료되지 않았습니다.");
		}
	}

	private String resolveName(Jwt jwt, String email) {
		String name = jwt.getClaimAsString("name");
		if (StringUtils.hasText(name)) {
			return name.strip();
		}
		return email.substring(0, email.indexOf('@'));
	}

	private String resolveClientId() {
		if (!StringUtils.hasText(properties.clientId())) {
			throw new BootSignalException(ErrorCode.INTERNAL_SERVER_ERROR, "Google Client ID 설정이 필요합니다.");
		}
		return properties.clientId().strip();
	}

	private BootSignalException invalidToken(String message) {
		return new BootSignalException(ErrorCode.INVALID_OAUTH_TOKEN, message);
	}
}
