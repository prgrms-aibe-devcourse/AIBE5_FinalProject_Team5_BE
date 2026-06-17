package com.bootsignal.domain.comment.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;
import com.bootsignal.domain.user.repository.UserRepository;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실제 회원가입/로그인 JWT 흐름으로 댓글 컨트롤러 API의 인증, 권한, 조회 동작을 검증합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommentControllerAuthIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Test
	void createListUpdateAndDeleteCommentWithLoginAccessToken() throws Exception {
		String writerToken = signupAndLogin("comment-writer@example.com", "commentWriter");
		Long postId = createPost(writerToken);
		Long commentId = createComment(writerToken, postId, "첫 댓글");

		mockMvc.perform(get("/api/posts/{postId}/comments", postId)
				.queryParam("page", "0")
				.queryParam("size", "10")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content[0].commentId").value(commentId))
			.andExpect(jsonPath("$.data.content[0].content").value("첫 댓글"))
			.andExpect(jsonPath("$.data.content[0].userNickname").value("commentWriter"))
			.andExpect(jsonPath("$.error").doesNotExist());

		mockMvc.perform(patch("/api/comments/{commentId}", commentId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + writerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"content": "수정된 댓글"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content").value("수정된 댓글"));

		mockMvc.perform(delete("/api/comments/{commentId}", commentId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + writerToken))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/posts/{postId}/comments", postId)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.totalElements").value(0));
	}

	@Test
	void createCommentWithoutAccessTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/posts/{postId}/comments", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"content": "댓글 내용"
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void createCommentWithBlankContentReturnsValidationError() throws Exception {
		String writerToken = signupAndLogin("blank-writer@example.com", "blankWriter");
		Long postId = createPost(writerToken);

		mockMvc.perform(post("/api/posts/{postId}/comments", postId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + writerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"content": "   "
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void updateCommentByNonAuthorReturnsForbidden() throws Exception {
		String writerToken = signupAndLogin("owner-writer@example.com", "ownerWriter");
		String otherToken = signupAndLogin("other-writer@example.com", "otherWriter");
		Long postId = createPost(writerToken);
		Long commentId = createComment(writerToken, postId, "작성자 댓글");

		mockMvc.perform(patch("/api/comments/{commentId}", commentId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"content": "다른 사용자의 수정"
					}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void deleteCommentByAdminSucceeds() throws Exception {
		String writerToken = signupAndLogin("admin-target@example.com", "adminTarget");
		Long postId = createPost(writerToken);
		Long commentId = createComment(writerToken, postId, "관리자 삭제 대상 댓글");

		signup("comment-admin@example.com", "commentAdmin");
		User admin = userRepository.findByEmail("comment-admin@example.com").orElseThrow();
		ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
		userRepository.flush();
		String adminToken = login("comment-admin@example.com");

		mockMvc.perform(delete("/api/comments/{commentId}", commentId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/posts/{postId}/comments", postId)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalElements").value(0));
	}

	private String signupAndLogin(String email, String nickname) throws Exception {
		signup(email, nickname);
		return login(email);
	}

	private void signup(String email, String nickname) throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "%s",
						"password": "password123",
						"nickname": "%s"
					}
					""".formatted(email, nickname)))
			.andExpect(status().isCreated());
	}

	private String login(String email) throws Exception {
		String loginResponse = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "%s",
						"password": "password123"
					}
					""".formatted(email)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		return objectMapper.readTree(loginResponse)
			.path("data")
			.path("accessToken")
			.asText();
	}

	private Long createPost(String accessToken) throws Exception {
		String response = mockMvc.perform(post("/api/posts")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"postType": "BOARD",
						"category": "자유",
						"title": "댓글 테스트 게시글",
						"content": "댓글 테스트 게시글 내용"
					}
					"""))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		return readDataLong(response, "postId");
	}

	private Long createComment(String accessToken, Long postId, String content) throws Exception {
		String response = mockMvc.perform(post("/api/posts/{postId}/comments", postId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"content": "%s"
					}
					""".formatted(content)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		return readDataLong(response, "commentId");
	}

	private Long readDataLong(String response, String fieldName) throws Exception {
		JsonNode root = objectMapper.readTree(response);
		return root.path("data").path(fieldName).asLong();
	}
}
