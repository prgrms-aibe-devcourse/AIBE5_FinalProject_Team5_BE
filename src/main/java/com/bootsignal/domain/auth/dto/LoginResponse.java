package com.bootsignal.domain.auth.dto;

import com.bootsignal.domain.user.entity.AuthProvider;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;
import com.bootsignal.global.security.jwt.JwtTokenPair;

/**
 * 로그인과 토큰 재발급 성공 시 사용자 정보와 JWT 토큰 정보를 함께 반환하는 DTO입니다.
 */
public record LoginResponse(
	Long userId,
	String email,
	String name,
	String nickname,
	UserRole role,
	AuthProvider provider,
	String tokenType,
	String accessToken,
	long accessTokenExpiresIn,
	String refreshToken,
	long refreshTokenExpiresIn
) {

	public LoginResponse(
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
		this(
			userId,
			email,
			nickname,
			nickname,
			role,
			AuthProvider.LOCAL,
			tokenType,
			accessToken,
			accessTokenExpiresIn,
			refreshToken,
			refreshTokenExpiresIn
		);
	}

	public static LoginResponse of(User user, JwtTokenPair tokenPair) {
		return new LoginResponse(
			user.getId(),
			user.getEmail(),
			user.getName(),
			user.getNickname(),
			user.getRole(),
			user.getProvider(),
			tokenPair.tokenType(),
			tokenPair.accessToken(),
			tokenPair.accessTokenExpiresIn(),
			tokenPair.refreshToken(),
			tokenPair.refreshTokenExpiresIn()
		);
	}
}
