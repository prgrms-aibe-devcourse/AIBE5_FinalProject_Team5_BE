package com.bootsignal.domain.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.auth.dto.AuthActionResponse;
import com.bootsignal.domain.auth.dto.EmailAvailabilityResponse;
import com.bootsignal.domain.auth.dto.LoginResponse;
import com.bootsignal.domain.auth.dto.PasswordForgotResponse;
import com.bootsignal.domain.auth.dto.SignupResponse;
import com.bootsignal.domain.auth.service.AuthService;
import com.bootsignal.domain.user.entity.UserRole;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.time.Instant;
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
			.andExpect(jsonPath("$.data.name").value("tester"))
			.andExpect(jsonPath("$.data.nickname").value("tester"))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	void checkEmailReturnsAvailability() throws Exception {
		given(authService.checkEmailAvailability("user@example.com"))
			.willReturn(new EmailAvailabilityResponse("user@example.com", true));

		mockMvc.perform(get("/api/auth/check-email")
				.queryParam("email", "user@example.com"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.email").value("user@example.com"))
			.andExpect(jsonPath("$.data.available").value(true))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	void forgotPasswordReturnsAcceptedResponse() throws Exception {
		Instant expiresAt = Instant.parse("2026-06-16T03:00:00Z");
		given(authService.requestPasswordReset(any()))
			.willReturn(new PasswordForgotResponse(true, "reset-token", expiresAt, 1800L));

		mockMvc.perform(post("/api/auth/password/forgot")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "user@example.com"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.accepted").value(true))
			.andExpect(jsonPath("$.data.resetToken").value("reset-token"))
			.andExpect(jsonPath("$.data.expiresInSeconds").value(1800))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	void resetPasswordReturnsCompletedResponse() throws Exception {
		given(authService.resetPassword(any()))
			.willReturn(AuthActionResponse.success());

		mockMvc.perform(post("/api/auth/password/reset")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"token": "reset-token",
						"newPassword": "newPassword123"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.completed").value(true))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	void loginReturnsOkResponse() throws Exception {
		given(authService.login(any()))
			.willReturn(new LoginResponse(
				1L,
				"user@example.com",
				"tester",
				UserRole.USER,
				"Bearer",
				"access-token",
				3600L,
				"refresh-token",
				1209600L
			));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "user@example.com",
						"password": "password123"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.userId").value(1))
			.andExpect(jsonPath("$.data.email").value("user@example.com"))
			.andExpect(jsonPath("$.data.nickname").value("tester"))
			.andExpect(jsonPath("$.data.role").value("USER"))
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.accessToken").value("access-token"))
			.andExpect(jsonPath("$.data.accessTokenExpiresIn").value(3600))
			.andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
			.andExpect(jsonPath("$.data.refreshTokenExpiresIn").value(1209600))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	void googleLoginReturnsOkResponse() throws Exception {
		given(authService.googleLogin(any()))
			.willReturn(new LoginResponse(
				1L,
				"user@example.com",
				"tester",
				UserRole.USER,
				"Bearer",
				"access-token",
				3600L,
				"refresh-token",
				1209600L
			));

		mockMvc.perform(post("/api/auth/google/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"idToken": "google-id-token"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.userId").value(1))
			.andExpect(jsonPath("$.data.email").value("user@example.com"))
			.andExpect(jsonPath("$.data.nickname").value("tester"))
			.andExpect(jsonPath("$.data.role").value("USER"))
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.accessToken").value("access-token"))
			.andExpect(jsonPath("$.data.accessTokenExpiresIn").value(3600))
			.andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
			.andExpect(jsonPath("$.data.refreshTokenExpiresIn").value(1209600))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	void kakaoLoginReturnsOkResponse() throws Exception {
		given(authService.kakaoLogin(any()))
			.willReturn(new LoginResponse(
				1L,
				"user@example.com",
				"tester",
				UserRole.USER,
				"Bearer",
				"access-token",
				3600L,
				"refresh-token",
				1209600L
			));

		mockMvc.perform(post("/api/auth/kakao/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"idToken": "kakao-id-token"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.userId").value(1))
			.andExpect(jsonPath("$.data.email").value("user@example.com"))
			.andExpect(jsonPath("$.data.nickname").value("tester"))
			.andExpect(jsonPath("$.data.role").value("USER"))
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.accessToken").value("access-token"))
			.andExpect(jsonPath("$.data.accessTokenExpiresIn").value(3600))
			.andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
			.andExpect(jsonPath("$.data.refreshTokenExpiresIn").value(1209600))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	void refreshReturnsOkResponse() throws Exception {
		given(authService.refresh(any()))
			.willReturn(new LoginResponse(
				1L,
				"user@example.com",
				"tester",
				UserRole.USER,
				"Bearer",
				"new-access-token",
				3600L,
				"new-refresh-token",
				1209600L
			));

		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"refreshToken": "refresh-token"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.userId").value(1))
			.andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
			.andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	void logoutReturnsNoContent() throws Exception {
		mockMvc.perform(post("/api/auth/logout")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"refreshToken": "refresh-token"
					}
					"""))
			.andExpect(status().isNoContent())
			.andReturn();

		verify(authService).logout(any());
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
	void loginReturnsValidationErrorWhenRequestIsInvalid() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "invalid-email",
						"password": "short"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.error.fieldErrors").isArray());

		verify(authService, never()).login(any());
	}

	@Test
	void googleLoginReturnsValidationErrorWhenRequestIsInvalid() throws Exception {
		mockMvc.perform(post("/api/auth/google/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"idToken": ""
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.error.fieldErrors").isArray());

		verify(authService, never()).googleLogin(any());
	}

	@Test
	void kakaoLoginReturnsValidationErrorWhenRequestIsInvalid() throws Exception {
		mockMvc.perform(post("/api/auth/kakao/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"idToken": ""
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.error.fieldErrors").isArray());

		verify(authService, never()).kakaoLogin(any());
	}

	@Test
	void refreshReturnsValidationErrorWhenRequestIsInvalid() throws Exception {
		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"refreshToken": ""
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.error.fieldErrors").isArray());

		verify(authService, never()).refresh(any());
	}

	@Test
	void logoutReturnsValidationErrorWhenRequestIsInvalid() throws Exception {
		mockMvc.perform(post("/api/auth/logout")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"refreshToken": ""
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.error.fieldErrors").isArray());

		verify(authService, never()).logout(any());
	}

	@Test
	void loginReturnsUnauthorizedWhenCredentialsAreInvalid() throws Exception {
		given(authService.login(any()))
			.willThrow(new BootSignalException(ErrorCode.INVALID_LOGIN_CREDENTIALS));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "user@example.com",
						"password": "password123"
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("INVALID_LOGIN_CREDENTIALS"))
			.andExpect(jsonPath("$.error.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
	}

	@Test
	void googleLoginReturnsUnauthorizedWhenTokenIsInvalid() throws Exception {
		given(authService.googleLogin(any()))
			.willThrow(new BootSignalException(ErrorCode.INVALID_OAUTH_TOKEN));

		mockMvc.perform(post("/api/auth/google/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"idToken": "invalid-token"
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("INVALID_OAUTH_TOKEN"));
	}

	@Test
	void kakaoLoginReturnsUnauthorizedWhenTokenIsInvalid() throws Exception {
		given(authService.kakaoLogin(any()))
			.willThrow(new BootSignalException(ErrorCode.INVALID_OAUTH_TOKEN));

		mockMvc.perform(post("/api/auth/kakao/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"idToken": "invalid-token"
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("INVALID_OAUTH_TOKEN"));
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
