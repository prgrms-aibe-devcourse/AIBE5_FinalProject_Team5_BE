package com.bootsignal.domain.calendar.service;

import com.bootsignal.global.config.properties.JwtProperties;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

// 구글 캘린더 최초 연동 처리

@Service
@RequiredArgsConstructor
public class GoogleCalendarOAuthStateService {

	private static final String STATE_TOKEN_USE = "calendar_connect_state";
	private static final long STATE_VALIDITY_SECONDS = 600;

	private final JwtProperties jwtProperties;


	/* 구글 캘린더 OAuth 상태 토큰 생성 (연동)*/
	public String createState(Long userId) {
		Instant now = Instant.now();

		return Jwts.builder()
			.issuer(jwtProperties.issuer())
			.subject(String.valueOf(userId))
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plusSeconds(STATE_VALIDITY_SECONDS)))
			.claim("token_use", STATE_TOKEN_USE)
			.signWith(signingKey())
			.compact();
	}

	/* 구글 캘린더 OAuth 상태 토큰 검증 및 사용자 ID 추출 (콜백) */
	public Long verifyAndExtractUserId(String state) {
		// 상태 토큰 유효성 검증
		if (!StringUtils.hasText(state)) {
			throw new BootSignalException(ErrorCode.CALENDAR_OAUTH_STATE_INVALID);
		}

		// 상태 토큰 페이로드 파싱
		Claims claims = parseClaims(state.strip());
		if (!STATE_TOKEN_USE.equals(claims.get("token_use", String.class))) {
			throw new BootSignalException(ErrorCode.CALENDAR_OAUTH_STATE_INVALID);
		}

		// 사용자 ID 추출 및 응답 반환
		try {
			return Long.parseLong(claims.getSubject());
		} catch (NumberFormatException exception) {
			throw new BootSignalException(ErrorCode.CALENDAR_OAUTH_STATE_INVALID);
		}
	}

	// 구글 캘린더 OAuth 상태 토큰 페이로드 파싱
	private Claims parseClaims(String state) {
		try {
			return Jwts.parser()
				.verifyWith(signingKey())
				.requireIssuer(jwtProperties.issuer())
				.build()
				.parseSignedClaims(state)
				.getPayload();
		} catch (ExpiredJwtException exception) {
			throw new BootSignalException(ErrorCode.CALENDAR_OAUTH_STATE_INVALID, "캘린더 연동 요청이 만료되었습니다.");
		} catch (JwtException | IllegalArgumentException exception) {
			throw new BootSignalException(ErrorCode.CALENDAR_OAUTH_STATE_INVALID);
		}
	}

	// 상태 토큰 서명 키 생성
	private SecretKey signingKey() {
		return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}
}
