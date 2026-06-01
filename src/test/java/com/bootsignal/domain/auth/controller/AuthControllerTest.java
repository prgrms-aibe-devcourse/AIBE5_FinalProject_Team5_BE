package com.bootsignal.domain.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.auth.dto.SignupResponse;
import com.bootsignal.domain.auth.service.AuthService;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@Test
	void signupReturnsCreatedResponse() throws Exception {
		given(authService.signup(any()))
			.willReturn(new SignupResponse(1L, "user@example.com", "tester"));

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "user@example.com",
						"password": "password123",
						"nickname": "tester"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(1))
			.andExpect(jsonPath("$.data.email").value("user@example.com"))
			.andExpect(jsonPath("$.data.nickname").value("tester"))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	void signupReturnsValidationErrorWhenRequestIsInvalid() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "invalid-email",
						"password": "short",
						"nickname": ""
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.error.fieldErrors").isArray());

		verify(authService, never()).signup(any());
	}

	@Test
	void signupReturnsConflictWhenEmailIsDuplicated() throws Exception {
		given(authService.signup(any()))
			.willThrow(new BootSignalException(ErrorCode.DUPLICATE_EMAIL));

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "user@example.com",
						"password": "password123",
						"nickname": "tester"
					}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("DUPLICATE_EMAIL"))
			.andExpect(jsonPath("$.error.message").value("이미 가입된 이메일입니다."));
	}

	@Test
	void signupReturnsConflictWhenNicknameIsDuplicated() throws Exception {
		given(authService.signup(any()))
			.willThrow(new BootSignalException(ErrorCode.DUPLICATE_NICKNAME));

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "user@example.com",
						"password": "password123",
						"nickname": "tester"
					}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("DUPLICATE_NICKNAME"))
			.andExpect(jsonPath("$.error.message").value("이미 사용 중인 닉네임입니다."));
	}
}
