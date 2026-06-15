package com.bootsignal.global.security.jwt;

import java.time.Instant;

/**
 * 검증된 Refresh Token에서 서비스가 필요한 사용자 식별자와 만료 시각만 담는 값 객체입니다.
 */
public record JwtRefreshTokenClaims(
	Long userId,
	Instant expiresAt
) {
}
