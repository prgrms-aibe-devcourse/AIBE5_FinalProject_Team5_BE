package com.bootsignal.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 재설정 토큰과 새 비밀번호를 전달받는 DTO입니다.
 */
public record PasswordResetRequest(
	@NotBlank(message = "비밀번호 재설정 토큰은 필수입니다.")
	String token,

	@NotBlank(message = "새 비밀번호는 필수입니다.")
	@Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하이어야 합니다.")
	String newPassword
) {
}
