package com.bootsignal.domain.user.dto;

import com.bootsignal.domain.user.entity.AuthProvider;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;

/**
 * 로그인 사용자의 프로필, 권한, 가입 방식을 반환하는 DTO입니다.
 */
public record UserInfoResponse(
	Long userId,
	String email,
	String name,
	String nickname,
	UserRole role,
	AuthProvider provider,
	String profileImageUrl
) {

	public UserInfoResponse(
		Long userId,
		String email,
		String nickname,
		UserRole role,
		AuthProvider provider,
		String profileImageUrl
	) {
		this(userId, email, nickname, nickname, role, provider, profileImageUrl);
	}

	public static UserInfoResponse from(User user) {
		return new UserInfoResponse(
			user.getId(),
			user.getEmail(),
			user.getName(),
			user.getNickname(),
			user.getRole(),
			user.getProvider(),
			user.getProfileImageUrl()
		);
	}
}
