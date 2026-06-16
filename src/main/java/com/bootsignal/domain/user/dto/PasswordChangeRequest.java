package com.bootsignal.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 로그인 사용자의 현재 비밀번호와 새 비밀번호를 전달받는 DTO입니다.
 */
public record PasswordChangeRequest(
	@NotBlank(message = "현재 비밀번호는 필수입니다.")
	@Size(min = 8, max = 64, message = "현재 비밀번호는 8자 이상 64자 이하이어야 합니다.")
	String currentPassword,

	@NotBlank(message = "새 비밀번호는 필수입니다.")
	@Size(min = 8, max = 64, message = "새 비밀번호는 8자 이상 64자 이하이어야 합니다.")
	String newPassword
) {
}
