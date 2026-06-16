package com.bootsignal.domain.auth.dto;

/**
 * 비밀번호 찾기 요청 접수 결과를 반환하는 DTO입니다.
 * 재설정 토큰 원문은 응답에 포함하지 않고 별도 전달 채널에서만 다룹니다.
 */
public record PasswordForgotResponse(
	boolean accepted,
	long expiresInSeconds,
	String resetToken,
	String resetUrl
) {

	public static PasswordForgotResponse accepted(long expiresInSeconds) {
		return new PasswordForgotResponse(true, expiresInSeconds, null, null);
	}

	public static PasswordForgotResponse acceptedWithToken(
		long expiresInSeconds,
		String resetToken,
		String resetUrl
	) {
		return new PasswordForgotResponse(true, expiresInSeconds, resetToken, resetUrl);
	}
}
