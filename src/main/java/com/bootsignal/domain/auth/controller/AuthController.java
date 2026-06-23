package com.bootsignal.domain.auth.controller;

import com.bootsignal.domain.auth.dto.AuthActionResponse;
import com.bootsignal.domain.auth.dto.EmailAvailabilityResponse;
import com.bootsignal.domain.auth.dto.GoogleLoginRequest;
import com.bootsignal.domain.auth.dto.KakaoLoginRequest;
import com.bootsignal.domain.auth.dto.LoginRequest;
import com.bootsignal.domain.auth.dto.LoginResponse;
import com.bootsignal.domain.auth.dto.PasswordForgotRequest;
import com.bootsignal.domain.auth.dto.PasswordForgotResponse;
import com.bootsignal.domain.auth.dto.PasswordResetRequest;
import com.bootsignal.domain.auth.dto.RefreshTokenRequest;
import com.bootsignal.domain.auth.dto.SignupRequest;
import com.bootsignal.domain.auth.dto.SignupResponse;
import com.bootsignal.domain.auth.service.AuthService;
import com.bootsignal.global.response.ApiResponse;
import com.bootsignal.global.security.jwt.JwtTokenCookieManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원가입, 로그인, 소셜 로그인, HttpOnly 쿠키 기반 토큰 관리, 비밀번호 찾기 API를 제공하는 인증 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

	private final AuthService authService;
	private final JwtTokenCookieManager jwtTokenCookieManager;

	@GetMapping("/check-email")
	public EmailAvailabilityResponse checkEmail(
		@RequestParam
		@NotBlank(message = "이메일은 필수입니다.")
		@Email(message = "이메일 형식이 올바르지 않습니다.")
		@Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
		String email
	) {
		return authService.checkEmailAvailability(email);
	}

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
		return authService.signup(request);
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
		LoginResponse loginResponse = authService.login(request);
		jwtTokenCookieManager.addTokenCookies(response, loginResponse);
		return loginResponse;
	}

	@PostMapping("/google/login")
	public LoginResponse googleLogin(@Valid @RequestBody GoogleLoginRequest request, HttpServletResponse response) {
		LoginResponse loginResponse = authService.googleLogin(request);
		jwtTokenCookieManager.addTokenCookies(response, loginResponse);
		return loginResponse;
	}

	@PostMapping("/kakao/login")
	public LoginResponse kakaoLogin(@Valid @RequestBody KakaoLoginRequest request, HttpServletResponse response) {
		LoginResponse loginResponse = authService.kakaoLogin(request);
		jwtTokenCookieManager.addTokenCookies(response, loginResponse);
		return loginResponse;
	}

	@PostMapping("/refresh")
	public LoginResponse refresh(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = jwtTokenCookieManager.requireRefreshToken(request);
		LoginResponse loginResponse = authService.refresh(new RefreshTokenRequest(refreshToken));
		jwtTokenCookieManager.addTokenCookies(response, loginResponse);
		return loginResponse;
	}

	@PostMapping("/password/forgot")
	public PasswordForgotResponse forgotPassword(@Valid @RequestBody PasswordForgotRequest request) {
		return authService.requestPasswordReset(request);
	}

	@PostMapping("/password/reset")
	public AuthActionResponse resetPassword(@Valid @RequestBody PasswordResetRequest request) {
		return authService.resetPassword(request);
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		try {
			String refreshToken = jwtTokenCookieManager.requireRefreshToken(request);
			authService.logout(new RefreshTokenRequest(refreshToken));
		} finally {
			jwtTokenCookieManager.clearTokenCookies(response);
		}
	}

	// JS가 쿠키 도메인 불일치로 XSRF-TOKEN을 읽지 못하는 경우의 폴백 엔드포인트.
	// CORS로 허용된 오리진만 응답을 읽을 수 있으므로 토큰 노출 위험이 없다.
	@GetMapping("/csrf-token")
	public ApiResponse<String> csrfToken(HttpServletRequest request) {
		return ApiResponse.success(jwtTokenCookieManager.resolveCsrfToken(request));
	}
}
