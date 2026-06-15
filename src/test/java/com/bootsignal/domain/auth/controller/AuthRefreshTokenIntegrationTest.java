package com.bootsignal.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.auth.entity.RefreshToken;
import com.bootsignal.domain.auth.oauth.GoogleTokenVerifier;
import com.bootsignal.domain.auth.oauth.GoogleUserInfo;
import com.bootsignal.domain.auth.oauth.KakaoTokenVerifier;
import com.bootsignal.domain.auth.oauth.KakaoUserInfo;
import com.bootsignal.domain.auth.repository.RefreshTokenRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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
	private ObjectMapper objectMapper;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@MockitoBean
	private GoogleTokenVerifier googleTokenVerifier;

	@MockitoBean
	private KakaoTokenVerifier kakaoTokenVerifier;

	@Test
	void refreshTokenIsStoredAsHashAndCannotBeReusedAfterRefreshOrLogout() throws Exception {
		signup("security-flow@example.com", "password123", "securityUser");
		JsonNode loginData = login("security-flow@example.com", "password123");
		String firstRefreshToken = loginData.path("refreshToken").asText();

		List<RefreshToken> issuedTokens = refreshTokenRepository.findAll();
		assertThat(issuedTokens).hasSize(1);
		assertThat(issuedTokens.getFirst().getTokenHash()).hasSize(64);
		assertThat(issuedTokens.getFirst().getTokenHash()).isNotEqualTo(firstRefreshToken);

		JsonNode refreshData = refresh(firstRefreshToken);
		String rotatedRefreshToken = refreshData.path("refreshToken").asText();
		assertThat(rotatedRefreshToken).isNotEqualTo(firstRefreshToken);

		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshTokenJson(firstRefreshToken)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_REUSED"));

		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshTokenJson(rotatedRefreshToken)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_REUSED"));

		JsonNode secondLoginData = login("security-flow@example.com", "password123");
		String logoutRefreshToken = secondLoginData.path("refreshToken").asText();

		mockMvc.perform(post("/api/auth/logout")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshTokenJson(logoutRefreshToken)))
			.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshTokenJson(logoutRefreshToken)))
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
		JsonNode loginData = login("stale-authorization@example.com", "password123");
		String firstRefreshToken = loginData.path("refreshToken").asText();

		String refreshResponse = mockMvc.perform(post("/api/auth/refresh")
				.header(HttpHeaders.AUTHORIZATION, "Bearer stale-or-invalid-access-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshTokenJson(firstRefreshToken)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		JsonNode refreshData = objectMapper.readTree(refreshResponse).path("data");
		String rotatedRefreshToken = refreshData.path("refreshToken").asText();
		assertThat(rotatedRefreshToken).isNotEqualTo(firstRefreshToken);

		mockMvc.perform(post("/api/auth/logout")
				.header(HttpHeaders.AUTHORIZATION, "Bearer stale-or-invalid-access-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshTokenJson(rotatedRefreshToken)))
			.andExpect(status().isNoContent());
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

		JsonNode loginData = socialLogin("/api/auth/google/login", "google-id-token");
		String refreshToken = loginData.path("refreshToken").asText();

		JsonNode refreshData = refresh(refreshToken);
		String rotatedRefreshToken = refreshData.path("refreshToken").asText();
		assertThat(rotatedRefreshToken).isNotEqualTo(refreshToken);

		mockMvc.perform(post("/api/auth/logout")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshTokenJson(rotatedRefreshToken)))
			.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshTokenJson(rotatedRefreshToken)))
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

		JsonNode loginData = socialLogin("/api/auth/kakao/login", "kakao-id-token");
		String refreshToken = loginData.path("refreshToken").asText();

		JsonNode refreshData = refresh(refreshToken);
		String rotatedRefreshToken = refreshData.path("refreshToken").asText();
		assertThat(rotatedRefreshToken).isNotEqualTo(refreshToken);

		mockMvc.perform(post("/api/auth/logout")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshTokenJson(rotatedRefreshToken)))
			.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshTokenJson(rotatedRefreshToken)))
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

	private JsonNode login(String email, String password) throws Exception {
		String response = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "%s",
						"password": "%s"
					}
					""".formatted(email, password)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		return objectMapper.readTree(response).path("data");
	}

	private JsonNode socialLogin(String path, String idToken) throws Exception {
		String response = mockMvc.perform(post(path)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"idToken": "%s"
					}
					""".formatted(idToken)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		return objectMapper.readTree(response).path("data");
	}

	private JsonNode refresh(String refreshToken) throws Exception {
		String response = mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshTokenJson(refreshToken)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		return objectMapper.readTree(response).path("data");
	}

	private String refreshTokenJson(String refreshToken) {
		return """
			{
				"refreshToken": "%s"
			}
			""".formatted(refreshToken);
	}
}
