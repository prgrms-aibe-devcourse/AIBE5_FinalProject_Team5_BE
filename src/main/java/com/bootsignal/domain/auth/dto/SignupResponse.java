package com.bootsignal.domain.auth.dto;

import com.bootsignal.domain.user.entity.AuthProvider;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 회원가입 완료 후 생성된 사용자 기본 정보를 반환하는 DTO입니다.
 */
public record SignupResponse(
	Long userId,
	String email,
	String name,
	String nickname,
	UserRole role,
	AuthProvider provider
) {

	public SignupResponse(Long id, String email, String nickname) {
		this(id, email, nickname, nickname, UserRole.USER, AuthProvider.LOCAL);
	}

	public static SignupResponse from(User user) {
		return new SignupResponse(
			user.getId(),
			user.getEmail(),
			user.getName(),
			user.getNickname(),
			user.getRole(),
			user.getProvider()
		);
	}

	@JsonIgnore
	public Long id() {
		return userId;
	}
}
