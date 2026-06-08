package com.bootsignal.domain.auth.oauth;

import com.bootsignal.global.config.properties.KakaoOAuthProperties;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.EmailFormatValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class KakaoIdTokenVerifier implements KakaoTokenVerifier {

	private static final String KAKAO_ISSUER = "https://kauth.kakao.com";

	private final KakaoOAuthProperties properties;
	private final JwtDecoder jwtDecoder;

	@Autowired
	public KakaoIdTokenVerifier(KakaoOAuthProperties properties) {
		this(properties, NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build());
	}

	KakaoIdTokenVerifier(KakaoOAuthProperties properties, JwtDecoder jwtDecoder) {
		this.properties = properties;
		this.jwtDecoder = jwtDecoder;
	}

	@Override
	public KakaoUserInfo verify(String idToken) {
		String clientId = resolveClientId();
		Jwt jwt = decode(idToken);

		validateIssuer(jwt);
		validateAudience(jwt, clientId);

		String email = EmailFormatValidator.normalize(jwt.getClaimAsString("email"));
		if (!EmailFormatValidator.isValid(email)) {
			throw invalidToken("Kakao ID Token의 이메일 정보가 올바르지 않습니다.");
		}

		String subject = jwt.getSubject();
		if (!StringUtils.hasText(subject)) {
			throw invalidToken("Kakao ID Token의 사용자 식별자가 없습니다.");
		}

		return new KakaoUserInfo(
			subject,
			email,
			resolveNickname(jwt, email),
			jwt.getClaimAsString("picture")
		);
	}

	private Jwt decode(String idToken) {
		try {
			return jwtDecoder.decode(idToken);
		} catch (JwtException | IllegalArgumentException exception) {
			throw invalidToken("유효하지 않은 Kakao ID Token입니다.");
		}
	}

	private void validateIssuer(Jwt jwt) {
		String issuer = jwt.getClaimAsString("iss");
		if (!KAKAO_ISSUER.equals(issuer)) {
			throw invalidToken("Kakao ID Token의 발급자가 올바르지 않습니다.");
		}
	}

	private void validateAudience(Jwt jwt, String clientId) {
		if (!jwt.getAudience().contains(clientId)) {
			throw invalidToken("Kakao ID Token의 대상자가 올바르지 않습니다.");
		}
	}

	private String resolveNickname(Jwt jwt, String email) {
		String nickname = jwt.getClaimAsString("nickname");
		if (StringUtils.hasText(nickname)) {
			return nickname.strip();
		}
		return email.substring(0, email.indexOf('@'));
	}

	private String resolveClientId() {
		if (!StringUtils.hasText(properties.clientId())) {
			throw new BootSignalException(ErrorCode.INTERNAL_SERVER_ERROR, "Kakao Client ID 설정이 필요합니다.");
		}
		return properties.clientId().strip();
	}

	private BootSignalException invalidToken(String message) {
		return new BootSignalException(ErrorCode.INVALID_OAUTH_TOKEN, message);
	}
}
