package com.bootsignal.domain.auth.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.auth.oauth.GoogleTokenVerifier;
import com.bootsignal.domain.auth.oauth.GoogleUserInfo;
import com.bootsignal.domain.auth.oauth.KakaoTokenVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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
 * 계정 관련 공개/인증 API가 실제 보안 필터와 DB를 거쳐 동작하는지 검증하는 통합 테스트입니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountManagementIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private GoogleTokenVerifier googleTokenVerifier;

	@MockitoBean
	private KakaoTokenVerifier kakaoTokenVerifier;

	@Test
	void localAccountCanUseEmailCheckPasswordChangeResetAndDeleteFlow() throws Exception {
		mockMvc.perform(get("/api/auth/check-email")
				.queryParam("email", "account-flow@example.com"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.available").value(true));

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "account-flow@example.com",
						"password": "password123",
						"name": "홍길동",
						"nickname": "accountFlow"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.name").value("홍길동"))
			.andExpect(jsonPath("$.data.nickname").value("accountFlow"));

		mockMvc.perform(get("/api/auth/check-email")
				.queryParam("email", "account-flow@example.com"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.available").value(false));

		String accessToken = login("account-flow@example.com", "password123").path("accessToken").asText();

		mockMvc.perform(patch("/api/members/me/password")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"currentPassword": "password123",
						"newPassword": "newPassword123"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.completed").value(true));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson("account-flow@example.com", "password123")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("INVALID_LOGIN_CREDENTIALS"));

		JsonNode forgotData = forgotPassword("account-flow@example.com");
		String resetToken = forgotData.path("resetToken").asText();

		mockMvc.perform(post("/api/auth/password/reset")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"token": "%s",
						"newPassword": "resetPassword123"
					}
					""".formatted(resetToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.completed").value(true));

		mockMvc.perform(post("/api/auth/password/reset")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"token": "%s",
						"newPassword": "anotherPassword123"
					}
					""".formatted(resetToken)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("PASSWORD_RESET_TOKEN_INVALID"));

		String resetAccessToken = login("account-flow@example.com", "resetPassword123").path("accessToken").asText();

		mockMvc.perform(delete("/api/members/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + resetAccessToken))
			.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson("account-flow@example.com", "resetPassword123")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("INVALID_LOGIN_CREDENTIALS"));
	}

	@Test
	void socialAccountPasswordForgotGuidesSocialLogin() throws Exception {
		given(googleTokenVerifier.verify("google-id-token"))
			.willReturn(new GoogleUserInfo(
				"google-subject",
				"social-reset@example.com",
				"소셜사용자",
				"https://example.com/profile.png"
			));

		mockMvc.perform(post("/api/auth/google/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"idToken": "google-id-token"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.provider").value("GOOGLE"));

		mockMvc.perform(post("/api/auth/password/forgot")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "social-reset@example.com"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("SOCIAL_LOGIN_REQUIRED"))
			.andExpect(jsonPath("$.error.message").value("소셜 로그인 계정입니다. 가입한 소셜 로그인으로 다시 로그인해 주세요."));
	}

	private JsonNode forgotPassword(String email) throws Exception {
		String response = mockMvc.perform(post("/api/auth/password/forgot")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "%s"
					}
					""".formatted(email)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accepted").value(true))
			.andExpect(jsonPath("$.data.resetToken").isNotEmpty())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		return objectMapper.readTree(response).path("data");
	}

	private JsonNode login(String email, String password) throws Exception {
		String response = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson(email, password)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		return objectMapper.readTree(response).path("data");
	}

	private String loginJson(String email, String password) {
		return """
			{
				"email": "%s",
				"password": "%s"
			}
			""".formatted(email, password);
	}
}
