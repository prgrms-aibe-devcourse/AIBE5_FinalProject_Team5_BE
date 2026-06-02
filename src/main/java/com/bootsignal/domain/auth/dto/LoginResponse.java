package com.bootsignal.domain.auth.dto;

import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;
import com.bootsignal.global.security.jwt.JwtTokenPair;

public record LoginResponse(
	Long userId,
	String email,
	String nickname,
	UserRole role,
	String tokenType,
	String accessToken,
	long accessTokenExpiresIn,
	String refreshToken,
	long refreshTokenExpiresIn
) {

	public static LoginResponse of(User user, JwtTokenPair tokenPair) {
		return new LoginResponse(
			user.getId(),
			user.getEmail(),
			user.getNickname(),
			user.getRole(),
			tokenPair.tokenType(),
			tokenPair.accessToken(),
			tokenPair.accessTokenExpiresIn(),
			tokenPair.refreshToken(),
			tokenPair.refreshTokenExpiresIn()
		);
	}
}
