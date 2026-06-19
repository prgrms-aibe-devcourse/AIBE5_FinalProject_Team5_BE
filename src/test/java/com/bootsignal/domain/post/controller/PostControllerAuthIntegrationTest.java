package com.bootsignal.domain.post.controller;

import static com.bootsignal.support.AuthCookieTestUtils.extractAccessToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 작성 API가 로그인 인증 토큰을 요구하고 인증 실패를 올바르게 처리하는지 검증하는 통합 테스트입니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostControllerAuthIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void createPostWithLoginAccessToken() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "writer@example.com",
						"password": "password123",
						"nickname": "writer"
					}
					"""))
			.andExpect(status().isCreated());

		String accessToken = extractAccessToken(mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "writer@example.com",
						"password": "password123"
					}
					"""))
			.andExpect(status().isOk())
			.andReturn());

		mockMvc.perform(post("/api/posts")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"postType": "BOARD",
						"category": "자유",
						"title": "게시글 제목",
						"content": "게시글 내용"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.userNickname").value("writer"))
			.andExpect(jsonPath("$.data.postType").value("BOARD"))
			.andExpect(jsonPath("$.data.title").value("게시글 제목"))
			.andExpect(jsonPath("$.data.content").value("게시글 내용"))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	void createPostWithoutAccessTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"postType": "BOARD",
						"title": "게시글 제목",
						"content": "게시글 내용"
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}
}
