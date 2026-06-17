package com.bootsignal.domain.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.bootsignal.global.config.properties.GoogleOAuthProperties;
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
class GoogleIdTokenVerifierTest {

	@Mock
	private JwtDecoder jwtDecoder;

	private GoogleIdTokenVerifier verifier;

	@BeforeEach
	void setUp() {
		verifier = new GoogleIdTokenVerifier(
			new GoogleOAuthProperties("google-client-id", "", "https://example.com/certs"),
			jwtDecoder
		);
	}

	@Test
	void verifyReturnsGoogleUserInfoWhenTokenIsValid() {
		given(jwtDecoder.decode("id-token")).willReturn(jwt(
			"https://accounts.google.com",
			List.of("google-client-id"),
			true,
			"google-sub",
			" User@Example.COM ",
			" Google User "
		));

		GoogleUserInfo userInfo = verifier.verify("id-token");

		assertThat(userInfo.subject()).isEqualTo("google-sub");
		assertThat(userInfo.email()).isEqualTo("user@example.com");
		assertThat(userInfo.name()).isEqualTo("Google User");
		assertThat(userInfo.pictureUrl()).isEqualTo("https://example.com/profile.png");
	}

	@Test
	void verifyUsesEmailLocalPartWhenNameIsBlank() {
		given(jwtDecoder.decode("id-token")).willReturn(jwt(
			"https://accounts.google.com",
			List.of("google-client-id"),
			true,
			"google-sub",
			"user@example.com",
			" "
		));

		GoogleUserInfo userInfo = verifier.verify("id-token");

		assertThat(userInfo.name()).isEqualTo("user");
	}

	@Test
	void verifyAcceptsGoogleIssuerWithoutScheme() {
		given(jwtDecoder.decode("id-token")).willReturn(jwt(
			"accounts.google.com",
			List.of("google-client-id"),
			true,
			"google-sub",
			"user@example.com",
			"Google User"
		));

		GoogleUserInfo userInfo = verifier.verify("id-token");

		assertThat(userInfo.subject()).isEqualTo("google-sub");
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
			List.of("google-client-id"),
			true,
			"google-sub",
			"user@example.com",
			"Google User"
		));

		assertInvalidOauthToken(() -> verifier.verify("id-token"));
	}

	@Test
	void verifyThrowsInvalidOauthTokenWhenAudienceIsInvalid() {
		given(jwtDecoder.decode("id-token")).willReturn(jwt(
			"https://accounts.google.com",
			List.of("another-client-id"),
			true,
			"google-sub",
			"user@example.com",
			"Google User"
		));

		assertInvalidOauthToken(() -> verifier.verify("id-token"));
	}

	@Test
	void verifyThrowsInvalidOauthTokenWhenEmailIsNotVerified() {
		given(jwtDecoder.decode("id-token")).willReturn(jwt(
			"https://accounts.google.com",
			List.of("google-client-id"),
			false,
			"google-sub",
			"user@example.com",
			"Google User"
		));

		assertInvalidOauthToken(() -> verifier.verify("id-token"));
	}

	@Test
	void verifyThrowsInvalidOauthTokenWhenEmailIsInvalid() {
		given(jwtDecoder.decode("id-token")).willReturn(jwt(
			"https://accounts.google.com",
			List.of("google-client-id"),
			true,
			"google-sub",
			"invalid-email",
			"Google User"
		));

		assertInvalidOauthToken(() -> verifier.verify("id-token"));
	}

	@Test
	void verifyThrowsInvalidOauthTokenWhenSubjectIsBlank() {
		given(jwtDecoder.decode("id-token")).willReturn(jwt(
			"https://accounts.google.com",
			List.of("google-client-id"),
			true,
			" ",
			"user@example.com",
			"Google User"
		));

		assertInvalidOauthToken(() -> verifier.verify("id-token"));
	}

	@Test
	void verifyThrowsInternalServerErrorWhenClientIdIsMissing() {
		GoogleIdTokenVerifier missingClientIdVerifier = new GoogleIdTokenVerifier(
			new GoogleOAuthProperties("", "", "https://example.com/certs"),
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
		boolean emailVerified,
		String subject,
		String email,
		String name
	) {
		return Jwt.withTokenValue("id-token")
			.header("alg", "RS256")
			.issuer(issuer)
			.audience(audience)
			.subject(subject)
			.claim("email", email)
			.claim("email_verified", emailVerified)
			.claim("name", name)
			.claim("picture", "https://example.com/profile.png")
			.build();
	}
}
