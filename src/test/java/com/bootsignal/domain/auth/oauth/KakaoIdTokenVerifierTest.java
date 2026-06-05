package com.bootsignal.domain.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.bootsignal.global.config.properties.KakaoOAuthProperties;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@ExtendWith(MockitoExtension.class)
class KakaoIdTokenVerifierTest {

	@Mock
	private JwtDecoder jwtDecoder;

	private KakaoIdTokenVerifier verifier;

	@BeforeEach
	void setUp() {
		verifier = new KakaoIdTokenVerifier(
			new KakaoOAuthProperties("kakao-client-id", "https://example.com/kakao-certs"),
			jwtDecoder
		);
	}

	@Test
	void verifyReturnsKakaoUserInfoWhenTokenIsValid() {
		given(jwtDecoder.decode("id-token")).willReturn(jwt(
			"https://kauth.kakao.com",
			List.of("kakao-client-id"),
			"kakao-sub",
			" User@Example.COM ",
			" Kakao User "
		));

		KakaoUserInfo userInfo = verifier.verify("id-token");

		assertThat(userInfo.subject()).isEqualTo("kakao-sub");
		assertThat(userInfo.email()).isEqualTo("user@example.com");
		assertThat(userInfo.nickname()).isEqualTo("Kakao User");
		assertThat(userInfo.pictureUrl()).isEqualTo("https://example.com/kakao-profile.png");
	}

	@Test
	void verifyUsesEmailLocalPartWhenNicknameIsBlank() {
		given(jwtDecoder.decode("id-token")).willReturn(jwt(
			"https://kauth.kakao.com",
			List.of("kakao-client-id"),
			"kakao-sub",
			"user@example.com",
			" "
		));

		KakaoUserInfo userInfo = verifier.verify("id-token");

		assertThat(userInfo.nickname()).isEqualTo("user");
	}

	@Test
	void verifyThrowsInvalidOauthTokenWhenDecoderFails() {
		given(jwtDecoder.decode("invalid-token")).willThrow(new BadJwtException("bad token"));

		assertInvalidOauthToken(() -> verifier.verify("invalid-token"));
	}

	@Test
	void verifyThrowsInvalidOauthTokenWhenIssuerIsInvalid() {
		given(jwtDecoder.decode("id-token")).willReturn(jwt(
			"https://evil.example.com",
			List.of("kakao-client-id"),
			"kakao-sub",
			"user@example.com",
			"Kakao User"
		));

		assertInvalidOauthToken(() -> verifier.verify("id-token"));
	}

	@Test
	void verifyThrowsInvalidOauthTokenWhenAudienceIsInvalid() {
		given(jwtDecoder.decode("id-token")).willReturn(jwt(
			"https://kauth.kakao.com",
			List.of("another-client-id"),
			"kakao-sub",
			"user@example.com",
			"Kakao User"
		));

		assertInvalidOauthToken(() -> verifier.verify("id-token"));
	}

	@Test
	void verifyThrowsInvalidOauthTokenWhenEmailIsInvalid() {
		given(jwtDecoder.decode("id-token")).willReturn(jwt(
			"https://kauth.kakao.com",
			List.of("kakao-client-id"),
			"kakao-sub",
			"invalid-email",
			"Kakao User"
		));

		assertInvalidOauthToken(() -> verifier.verify("id-token"));
	}

	@Test
	void verifyThrowsInvalidOauthTokenWhenSubjectIsBlank() {
		given(jwtDecoder.decode("id-token")).willReturn(jwt(
			"https://kauth.kakao.com",
			List.of("kakao-client-id"),
			" ",
			"user@example.com",
			"Kakao User"
		));

		assertInvalidOauthToken(() -> verifier.verify("id-token"));
	}

	@Test
	void verifyThrowsInternalServerErrorWhenClientIdIsMissing() {
		KakaoIdTokenVerifier missingClientIdVerifier = new KakaoIdTokenVerifier(
			new KakaoOAuthProperties("", "https://example.com/kakao-certs"),
			jwtDecoder
		);

		assertThatThrownBy(() -> missingClientIdVerifier.verify("id-token"))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
	}

	private void assertInvalidOauthToken(Runnable runnable) {
		assertThatThrownBy(runnable::run)
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.INVALID_OAUTH_TOKEN);
	}

	private Jwt jwt(
		String issuer,
		List<String> audience,
		String subject,
		String email,
		String nickname
	) {
		return Jwt.withTokenValue("id-token")
			.header("alg", "RS256")
			.issuer(issuer)
			.audience(audience)
			.subject(subject)
			.claim("email", email)
			.claim("nickname", nickname)
			.claim("picture", "https://example.com/kakao-profile.png")
			.build();
	}
}
