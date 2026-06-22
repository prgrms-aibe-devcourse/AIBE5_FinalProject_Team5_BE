package com.bootsignal.domain.auth.dto;

import com.bootsignal.domain.user.entity.AuthProvider;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;
import com.bootsignal.global.security.jwt.JwtTokenPair;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 로그인과 토큰 재발급 성공 시 쿠키 발급에 필요한 JWT와 응답 본문의 사용자 정보를 함께 운반하는 DTO입니다.
 */
public record LoginResponse(
	@JsonIgnore
	String accessToken,
	@JsonIgnore
	String tokenType,
	@JsonIgnore
	long expiresIn,
	@JsonIgnore
	String refreshToken,
	@JsonIgnore
	long refreshTokenExpiresIn,
	AuthUserResponse user
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
			accessToken,
			tokenType,
			accessTokenExpiresIn,
			refreshToken,
			refreshTokenExpiresIn,
			new AuthUserResponse(userId, email, nickname, nickname, role, AuthProvider.LOCAL)
		);
	}

	public static LoginResponse of(User user, JwtTokenPair tokenPair) {
		return new LoginResponse(
			tokenPair.accessToken(),
			tokenPair.tokenType(),
			tokenPair.accessTokenExpiresIn(),
			tokenPair.refreshToken(),
			tokenPair.refreshTokenExpiresIn(),
			AuthUserResponse.from(user)
		);
	}

	@JsonIgnore
	public long accessTokenExpiresIn() {
		return expiresIn;
	}

	@JsonIgnore
	public Long userId() {
		return user.userId();
	}

	@JsonIgnore
	public String email() {
		return user.email();
	}

	@JsonIgnore
	public String name() {
		return user.name();
	}

	@JsonIgnore
	public String nickname() {
		return user.nickname();
	}

	@JsonIgnore
	public UserRole role() {
		return user.role();
	}

	@JsonIgnore
	public AuthProvider provider() {
		return user.provider();
	}
}
