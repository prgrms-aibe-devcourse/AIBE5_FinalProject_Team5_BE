package com.bootsignal.domain.user.dto;

import com.bootsignal.domain.user.entity.AuthProvider;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;

/** 사용자 정보 조회 응답  **/
public record UserInfoResponse(
	Long userId,
	String email,
	String nickname,
	UserRole role,
	AuthProvider provider,
	String profileImageUrl
) {

	public static UserInfoResponse from(User user) {
		return new UserInfoResponse(
			user.getId(),
			user.getEmail(),
			user.getNickname(),
			user.getRole(),
			user.getProvider(),
			user.getProfileImageUrl()
		);
	}
}
