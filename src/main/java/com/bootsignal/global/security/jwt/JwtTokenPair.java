package com.bootsignal.global.security.jwt;

public record JwtTokenPair(
	String tokenType,
	String accessToken,
	long accessTokenExpiresIn,
	String refreshToken,
	long refreshTokenExpiresIn
) {
}
