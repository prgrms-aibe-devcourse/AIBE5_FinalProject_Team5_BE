package com.bootsignal.domain.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;
import com.bootsignal.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 API 인가 통합 테스트.
 * <p>
 * 1) JWT가 부여하는 권한(ROLE_ADMIN)과 컨트롤러의 @PreAuthorize("hasRole('ADMIN')") 표현식이 일치하는지,
 * 2) 인증되지 않은 요청은 401, 권한이 없는 사용자의 요청은 500이 아닌 403으로 응답되는지를 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminAuthorizationIntegrationTest {

	private static final String ADMIN_REPORTS_URL = "/api/admin/reports";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void nonAdminUserReceivesForbidden() throws Exception {
		signup("user@example.com", "password123", "일반회원");
		String accessToken = login("user@example.com", "password123");

		// USER 권한으로 관리자 API 접근 → 권한 거부는 500이 아니라 403이어야 한다.
		mockMvc.perform(get(ADMIN_REPORTS_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void adminUserCanAccessAdminApi() throws Exception {
		// User 팩토리는 항상 USER로 생성하므로, 테스트에서 ADMIN 권한을 주입해 저장한다.
		User admin = User.signupLocal("admin@example.com", passwordEncoder.encode("password123"), "관리자");
		ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
		userRepository.save(admin);

		String accessToken = login("admin@example.com", "password123");

		// 실제 로그인으로 발급된 JWT(ROLE_ADMIN)가 hasRole('ADMIN') 표현식을 통과해야 한다.
		mockMvc.perform(get(ADMIN_REPORTS_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	void requestWithoutTokenReceivesUnauthorized() throws Exception {
		// 인증되지 않은 요청은 Security 필터 단계에서 인증 실패로 처리되어 401을 반환한다.
		mockMvc.perform(get(ADMIN_REPORTS_URL))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
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

	private String login(String email, String password) throws Exception {
		String loginResponse = mockMvc.perform(post("/api/auth/login")
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

		return objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();
	}
}
