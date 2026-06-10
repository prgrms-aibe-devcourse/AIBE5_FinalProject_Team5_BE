package com.bootsignal.domain.user.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.user.dto.UserInfoResponse;
import com.bootsignal.domain.user.entity.AuthProvider;
import com.bootsignal.domain.user.entity.UserRole;
import com.bootsignal.domain.user.service.UserService;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// UserController 슬라이스 테스트 — Controller + ApiResponseAdvice만 로드, Service는 Mock
@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false) // JWT 필터 비활성화 (인증은 Service Mock으로 검증)
@DisplayName("UserController 테스트")
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@Test
	@DisplayName("GET /api/users/me — 내 정보 조회 성공 (200)")
	void getMyInfoReturnsOkResponse() throws Exception {
		// given
		given(userService.getMyInfo())
			.willReturn(new UserInfoResponse(
				1L,
				"user@example.com",
				"길동",
				UserRole.USER,
				AuthProvider.GOOGLE,
				"https://example.com/profile.png"
			));

		// when & then
		mockMvc.perform(get("/api/users/me"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.userId").value(1))
			.andExpect(jsonPath("$.data.email").value("user@example.com"))
			.andExpect(jsonPath("$.data.nickname").value("길동"))
			.andExpect(jsonPath("$.data.role").value("USER"))
			.andExpect(jsonPath("$.data.provider").value("GOOGLE"))
			.andExpect(jsonPath("$.data.profileImageUrl").value("https://example.com/profile.png"))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	@DisplayName("GET /api/users/me — 미로그인 시 401 UNAUTHORIZED")
	void getMyInfoReturnsUnauthorizedWhenNotLoggedIn() throws Exception {
		// given
		given(userService.getMyInfo())
			.willThrow(new BootSignalException(ErrorCode.UNAUTHORIZED));

		// when & then
		mockMvc.perform(get("/api/users/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
			.andExpect(jsonPath("$.error.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("GET /api/users/me — 사용자 없음 시 404 USER_NOT_FOUND")
	void getMyInfoReturnsNotFoundWhenUserDoesNotExist() throws Exception {
		// given
		given(userService.getMyInfo())
			.willThrow(new BootSignalException(ErrorCode.USER_NOT_FOUND));

		// when & then
		mockMvc.perform(get("/api/users/me"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"))
			.andExpect(jsonPath("$.error.message").value("사용자를 찾을 수 없습니다."));
	}
}
