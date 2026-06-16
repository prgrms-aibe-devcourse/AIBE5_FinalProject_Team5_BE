package com.bootsignal.domain.auth.dto;

import java.time.Instant;

/**
 * 비밀번호 찾기 요청 처리 결과와 재설정 토큰 정보를 반환하는 DTO입니다.
 */
public record PasswordForgotResponse(
	boolean accepted,
	String resetToken,
	Instant expiresAt,
	long expiresInSeconds
) {

	public static PasswordForgotResponse acceptedWithoutToken() {
		return new PasswordForgotResponse(true, null, null, 0);
	}
}
