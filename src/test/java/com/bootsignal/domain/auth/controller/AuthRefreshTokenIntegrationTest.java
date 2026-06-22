package com.bootsignal.domain.auth.controller;

import static com.bootsignal.support.AuthCookieTestUtils.extractAccessTokenCookie;
import static com.bootsignal.support.AuthCookieTestUtils.extractCsrfTokenCookie;
import static com.bootsignal.support.AuthCookieTestUtils.extractRefreshTokenCookie;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.auth.entity.RefreshToken;
import com.bootsignal.domain.auth.oauth.GoogleTokenVerifier;
import com.bootsignal.domain.auth.oauth.GoogleUserInfo;
import com.bootsignal.domain.auth.oauth.KakaoTokenVerifier;
import com.bootsignal.domain.auth.oauth.KakaoUserInfo;
import com.bootsignal.domain.auth.repository.RefreshTokenRepository;
import com.bootsignal.global.security.jwt.JwtTokenCookieManager;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh Token 회전, 재사용 차단, 로그아웃 무효화를 실제 API와 DB 상태로 검증하는 통합 테스트입니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthRefreshTokenIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@MockitoBean
	private GoogleTokenVerifier googleTokenVerifier;

	@MockitoBean
	private KakaoTokenVerifier kakaoTokenVerifier;

	@Test
	void refreshTokenIsStoredAsHashAndCannotBeReusedAfterRefreshOrLogout() throws Exception {
		signup("security-flow@example.com", "password123", "securityUser");
		AuthCookies loginCookies = login("security-flow@example.com", "password123");
		String firstRefreshToken = loginCookies.refreshToken().getValue();

		List<RefreshToken> issuedTokens = refreshTokenRepository.findAll();
		assertThat(issuedTokens).hasSize(1);
		assertThat(issuedTokens.getFirst().getTokenHash()).hasSize(64);
		assertThat(issuedTokens.getFirst().getTokenHash()).isNotEqualTo(firstRefreshToken);

		AuthCookies refreshedCookies = refresh(loginCookies);
		String rotatedRefreshToken = refreshedCookies.refreshToken().getValue();
		assertThat(rotatedRefreshToken).isNotEqualTo(firstRefreshToken);

		mockMvc.perform(post("/api/auth/refresh")
				.cookie(loginCookies.refreshToken(), loginCookies.csrfToken())
				.header(JwtTokenCookieManager.CSRF_TOKEN_HEADER_NAME, loginCookies.csrfToken().getValue()))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_REUSED"));

		mockMvc.perform(post("/api/auth/refresh")
				.cookie(refreshedCookies.refreshToken(), refreshedCookies.csrfToken())
				.header(JwtTokenCookieManager.CSRF_TOKEN_HEADER_NAME, refreshedCookies.csrfToken().getValue()))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_REUSED"));

		AuthCookies secondLoginCookies = login("security-flow@example.com", "password123");
		String logoutRefreshToken = secondLoginCookies.refreshToken().getValue();

		mockMvc.perform(post("/api/auth/logout")
				.cookie(secondLoginCookies.refreshToken(), secondLoginCookies.csrfToken())
				.header(JwtTokenCookieManager.CSRF_TOKEN_HEADER_NAME, secondLoginCookies.csrfToken().getValue()))
			.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/auth/refresh")
				.cookie(secondLoginCookies.refreshToken(), secondLoginCookies.csrfToken())
				.header(JwtTokenCookieManager.CSRF_TOKEN_HEADER_NAME, secondLoginCookies.csrfToken().getValue()))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_REUSED"));

		assertThat(refreshTokenRepository.findAll())
			.extracting(RefreshToken::getTokenHash)
			.doesNotContain(firstRefreshToken, rotatedRefreshToken, logoutRefreshToken);
	}

	@Test
	void refreshAndLogoutUseRefreshTokenEvenWhenAuthorizationHeaderIsStale() throws Exception {
		signup("stale-authorization@example.com", "password123", "staleAuthUser");
		AuthCookies loginCookies = login("stale-authorization@example.com", "password123");
		String firstRefreshToken = loginCookies.refreshToken().getValue();

		MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
				.header(HttpHeaders.AUTHORIZATION, "Bearer stale-or-invalid-access-token")
				.cookie(loginCookies.refreshToken()))
			.andExpect(status().isOk())
			.andReturn();

		AuthCookies refreshedCookies = extractAuthCookies(refreshResult);
		String rotatedRefreshToken = refreshedCookies.refreshToken().getValue();
		assertThat(rotatedRefreshToken).isNotEqualTo(firstRefreshToken);

		mockMvc.perform(post("/api/auth/logout")
				.header(HttpHeaders.AUTHORIZATION, "Bearer stale-or-invalid-access-token")
				.cookie(refreshedCookies.refreshToken()))
			.andExpect(status().isNoContent());
	}

	@Test
	void cookieAuthenticationRequiresCsrfTokenForUnsafeRequest() throws Exception {
		signup("csrf-cookie@example.com", "password123", "csrfCookieUser");
		AuthCookies loginCookies = login("csrf-cookie@example.com", "password123");

		mockMvc.perform(post("/api/posts")
				.cookie(loginCookies.accessToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content(postJson("CSRF 실패 게시글")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("CSRF_TOKEN_INVALID"));

		mockMvc.perform(post("/api/posts")
				.cookie(loginCookies.accessToken(), loginCookies.csrfToken())
				.header(JwtTokenCookieManager.CSRF_TOKEN_HEADER_NAME, loginCookies.csrfToken().getValue())
				.contentType(MediaType.APPLICATION_JSON)
				.content(postJson("CSRF 성공 게시글")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.title").value("CSRF 성공 게시글"));
	}

	@Test
	void accessTokenCookieAuthenticatesProtectedEndpoint() throws Exception {
		signup("cookie-auth@example.com", "password123", "cookieAuthUser");
		AuthCookies loginCookies = login("cookie-auth@example.com", "password123");

		mockMvc.perform(get("/api/users/me")
				.cookie(loginCookies.accessToken()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.email").value("cookie-auth@example.com"));
	}

	@Test
	void publicLoginApiAllowsExistingAuthCookieWithoutCsrfHeader() throws Exception {
		signup("legacy-cookie@example.com", "password123", "legacyCookieUser");
		AuthCookies loginCookies = login("legacy-cookie@example.com", "password123");

		mockMvc.perform(post("/api/auth/login")
				.cookie(loginCookies.refreshToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "legacy-cookie@example.com",
						"password": "password123"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));
	}

	@Test
	void googleLoginUserCanRefreshAndLogoutWithRefreshTokenFlow() throws Exception {
		given(googleTokenVerifier.verify("google-id-token"))
			.willReturn(new GoogleUserInfo(
				"google-subject",
				"google-social@example.com",
				"Google User",
				"https://example.com/google.png"
			));

		AuthCookies loginCookies = socialLogin("/api/auth/google/login", "google-id-token");
		String refreshToken = loginCookies.refreshToken().getValue();

		AuthCookies refreshedCookies = refresh(loginCookies);
		String rotatedRefreshToken = refreshedCookies.refreshToken().getValue();
		assertThat(rotatedRefreshToken).isNotEqualTo(refreshToken);

		mockMvc.perform(post("/api/auth/logout")
				.cookie(refreshedCookies.refreshToken(), refreshedCookies.csrfToken())
				.header(JwtTokenCookieManager.CSRF_TOKEN_HEADER_NAME, refreshedCookies.csrfToken().getValue()))
			.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/auth/refresh")
				.cookie(refreshedCookies.refreshToken(), refreshedCookies.csrfToken())
				.header(JwtTokenCookieManager.CSRF_TOKEN_HEADER_NAME, refreshedCookies.csrfToken().getValue()))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_REUSED"));
	}

	@Test
	void kakaoLoginUserCanRefreshAndLogoutWithRefreshTokenFlow() throws Exception {
		given(kakaoTokenVerifier.verify("kakao-id-token"))
			.willReturn(new KakaoUserInfo(
				"kakao-subject",
				"kakao-social@example.com",
				"Kakao User",
				"https://example.com/kakao.png"
			));

		AuthCookies loginCookies = socialLogin("/api/auth/kakao/login", "kakao-id-token");
		String refreshToken = loginCookies.refreshToken().getValue();

		AuthCookies refreshedCookies = refresh(loginCookies);
		String rotatedRefreshToken = refreshedCookies.refreshToken().getValue();
		assertThat(rotatedRefreshToken).isNotEqualTo(refreshToken);

		mockMvc.perform(post("/api/auth/logout")
				.cookie(refreshedCookies.refreshToken(), refreshedCookies.csrfToken())
				.header(JwtTokenCookieManager.CSRF_TOKEN_HEADER_NAME, refreshedCookies.csrfToken().getValue()))
			.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/auth/refresh")
				.cookie(refreshedCookies.refreshToken(), refreshedCookies.csrfToken())
				.header(JwtTokenCookieManager.CSRF_TOKEN_HEADER_NAME, refreshedCookies.csrfToken().getValue()))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_REUSED"));
	}

	private void signup(String email, String password, String nickname) throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "%s",
						"password": "%s",
						"nickname": "%s"
					}
					""".formatted(email, password, nickname)))
			.andExpect(status().isCreated());
	}

	private AuthCookies login(String email, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "%s",
						"password": "%s"
					}
					""".formatted(email, password)))
			.andExpect(status().isOk())
			.andReturn();

		return extractAuthCookies(result);
	}

	private AuthCookies socialLogin(String path, String idToken) throws Exception {
		MvcResult result = mockMvc.perform(post(path)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"idToken": "%s"
					}
					""".formatted(idToken)))
			.andExpect(status().isOk())
			.andReturn();

		return extractAuthCookies(result);
	}

	private AuthCookies refresh(AuthCookies authCookies) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/refresh")
				.cookie(authCookies.refreshToken(), authCookies.csrfToken())
				.header(JwtTokenCookieManager.CSRF_TOKEN_HEADER_NAME, authCookies.csrfToken().getValue()))
			.andExpect(status().isOk())
			.andReturn();

		return extractAuthCookies(result);
	}

	private AuthCookies extractAuthCookies(MvcResult result) {
		return new AuthCookies(
			extractAccessTokenCookie(result),
			extractRefreshTokenCookie(result),
			extractCsrfTokenCookie(result)
		);
	}

	private String postJson(String title) {
		return """
			{
				"postType": "BOARD",
				"category": "자유",
				"title": "%s",
				"content": "CSRF 검증용 게시글 내용"
			}
			""".formatted(title);
	}

	private record AuthCookies(
		Cookie accessToken,
		Cookie refreshToken,
		Cookie csrfToken
	) {
	}
}
