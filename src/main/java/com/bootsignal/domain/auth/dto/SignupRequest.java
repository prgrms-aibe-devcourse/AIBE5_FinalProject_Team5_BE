package com.bootsignal.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 일반 회원가입 요청에서 이메일, 비밀번호, 이름, 닉네임을 전달받는 DTO입니다.
 */
public record SignupRequest(
	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	@Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
	String email,

	@NotBlank(message = "비밀번호는 필수입니다.")
	@Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하이어야 합니다.")
	String password,

	@Size(max = 100, message = "이름은 100자 이하여야 합니다.")
	String name,

	@NotBlank(message = "닉네임은 필수입니다.")
	@Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
	String nickname
) {

	public SignupRequest(String email, String password, String nickname) {
		this(email, password, nickname, nickname);
	}
}
