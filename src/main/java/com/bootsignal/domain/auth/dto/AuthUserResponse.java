package com.bootsignal.domain.auth.dto;

import com.bootsignal.domain.user.entity.AuthProvider;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;

/**
 * 인증 응답에서 프론트가 세션 사용자로 저장할 기본 회원 정보를 담는 DTO입니다.
 */
public record AuthUserResponse(
	Long userId,
	String email,
	String name,
	String nickname,
	UserRole role,
	AuthProvider provider
) {

	public static AuthUserResponse from(User user) {
		return new AuthUserResponse(
			user.getId(),
			user.getEmail(),
			user.getName(),
			user.getNickname(),
			user.getRole(),
			user.getProvider()
		);
	}
}
