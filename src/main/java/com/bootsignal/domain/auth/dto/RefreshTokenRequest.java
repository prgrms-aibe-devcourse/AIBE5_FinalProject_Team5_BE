package com.bootsignal.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그아웃과 토큰 재발급 요청에서 refresh token을 전달받는 DTO입니다.
 */
public record RefreshTokenRequest(
	@NotBlank String refreshToken
) {
}
