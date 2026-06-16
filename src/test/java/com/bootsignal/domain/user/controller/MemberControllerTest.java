package com.bootsignal.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.user.dto.MemberActionResponse;
import com.bootsignal.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MemberController가 프론트 요구사항의 `/api/members` 계정 관리 경로를 제공하는지 검증합니다.
 */
@WebMvcTest(controllers = MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MemberController 테스트")
class MemberControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@Test
	@DisplayName("PATCH /api/members/me/password — 비밀번호 변경 성공 (200)")
	void changeMyPasswordReturnsOkResponse() throws Exception {
		given(userService.changePassword(any())).willReturn(MemberActionResponse.success());

		mockMvc.perform(patch("/api/members/me/password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"currentPassword": "password123",
						"newPassword": "newPassword123"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.completed").value(true))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	@DisplayName("DELETE /api/members/me — 회원 탈퇴 성공 (204)")
	void deleteMyAccountReturnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/members/me"))
			.andExpect(status().isNoContent());

		verify(userService).deleteMyAccount();
	}
}
