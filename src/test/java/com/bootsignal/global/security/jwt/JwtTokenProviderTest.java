package com.bootsignal.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bootsignal.domain.user.entity.User;
import com.bootsignal.global.config.properties.JwtProperties;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenProviderTest {

	private static final String SECRET = "test-jwt-secret-change-before-deploy-32bytes";

	@Test
	void createTokenPairReturnsSignedAccessAndRefreshTokens() {
		JwtProperties properties = new JwtProperties("bootsignal-test", SECRET, 3600L, 1209600L);
		JwtTokenProvider tokenProvider = new JwtTokenProvider(properties);
		User user = User.signupLocal("user@example.com", "encoded-password", "tester");
		ReflectionTestUtils.setField(user, "id", 1L);

		JwtTokenPair tokenPair = tokenProvider.createTokenPair(user);

		assertThat(tokenPair.tokenType()).isEqualTo("Bearer");
		assertThat(tokenPair.accessTokenExpiresIn()).isEqualTo(3600L);
		assertThat(tokenPair.refreshTokenExpiresIn()).isEqualTo(1209600L);

		Claims accessClaims = parseClaims(tokenPair.accessToken());
		assertThat(accessClaims.getIssuer()).isEqualTo("bootsignal-test");
		assertThat(accessClaims.getSubject()).isEqualTo("1");
		assertThat(accessClaims.get("email")).isEqualTo("user@example.com");
		assertThat(accessClaims.get("nickname")).isEqualTo("tester");
		assertThat(accessClaims.get("role")).isEqualTo("USER");
		assertThat(accessClaims.get("provider")).isEqualTo("LOCAL");
		assertThat(accessClaims.get("token_use")).isEqualTo("access");

		Claims refreshClaims = parseClaims(tokenPair.refreshToken());
		assertThat(refreshClaims.getSubject()).isEqualTo("1");
		assertThat(refreshClaims.get("token_use")).isEqualTo("refresh");
	}

	@Test
	void getAuthenticationReturnsEmailPrincipalFromAccessToken() {
		JwtProperties properties = new JwtProperties("bootsignal-test", SECRET, 3600L, 1209600L);
		JwtTokenProvider tokenProvider = new JwtTokenProvider(properties);
		User user = User.signupLocal("user@example.com", "encoded-password", "tester");
		ReflectionTestUtils.setField(user, "id", 1L);
		JwtTokenPair tokenPair = tokenProvider.createTokenPair(user);

		var authentication = tokenProvider.getAuthentication(tokenPair.accessToken());

		assertThat(authentication.getName()).isEqualTo("user@example.com");
		assertThat(authentication.getCredentials()).isEqualTo(tokenPair.accessToken());
		assertThat(authentication.getAuthorities())
			.extracting(GrantedAuthority::getAuthority)
			.containsExactly("ROLE_USER");
	}

	@Test
	void getAuthenticationRejectsRefreshToken() {
		JwtProperties properties = new JwtProperties("bootsignal-test", SECRET, 3600L, 1209600L);
		JwtTokenProvider tokenProvider = new JwtTokenProvider(properties);
		User user = User.signupLocal("user@example.com", "encoded-password", "tester");
		ReflectionTestUtils.setField(user, "id", 1L);
		JwtTokenPair tokenPair = tokenProvider.createTokenPair(user);

		assertThatThrownBy(() -> tokenProvider.getAuthentication(tokenPair.refreshToken()))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	void getRefreshTokenClaimsReturnsUserIdAndExpiration() {
		JwtProperties properties = new JwtProperties("bootsignal-test", SECRET, 3600L, 1209600L);
		JwtTokenProvider tokenProvider = new JwtTokenProvider(properties);
		User user = User.signupLocal("user@example.com", "encoded-password", "tester");
		ReflectionTestUtils.setField(user, "id", 1L);
		JwtTokenPair tokenPair = tokenProvider.createTokenPair(user);

		JwtRefreshTokenClaims claims = tokenProvider.getRefreshTokenClaims(tokenPair.refreshToken());

		assertThat(claims.userId()).isEqualTo(1L);
		assertThat(claims.expiresAt()).isNotNull();
	}

	@Test
	void getRefreshTokenClaimsRejectsAccessToken() {
		JwtProperties properties = new JwtProperties("bootsignal-test", SECRET, 3600L, 1209600L);
		JwtTokenProvider tokenProvider = new JwtTokenProvider(properties);
		User user = User.signupLocal("user@example.com", "encoded-password", "tester");
		ReflectionTestUtils.setField(user, "id", 1L);
		JwtTokenPair tokenPair = tokenProvider.createTokenPair(user);

		assertThatThrownBy(() -> tokenProvider.getRefreshTokenClaims(tokenPair.accessToken()))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
	}

	private Claims parseClaims(String token) {
		SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
		return Jwts.parser()
			.verifyWith(key)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}
}
