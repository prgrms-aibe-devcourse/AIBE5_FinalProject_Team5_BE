package com.bootsignal.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 재설정 토큰 발급을 요청할 때 이메일을 전달받는 DTO입니다.
 */
public record PasswordForgotRequest(
	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	@Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
	String email
) {
}
