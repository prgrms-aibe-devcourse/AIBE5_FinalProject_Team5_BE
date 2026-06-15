package com.bootsignal.global.security.jwt;

import com.bootsignal.domain.user.entity.User;
import com.bootsignal.global.config.properties.JwtProperties;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.EmailFormatValidator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * JWT access/refresh token을 발급하고 용도별 검증 결과를 제공합니다.
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

	private static final String TOKEN_TYPE = "Bearer";
	private static final String ACCESS_TOKEN_USE = "access";
	private static final String REFRESH_TOKEN_USE = "refresh";

	private final JwtProperties jwtProperties;

	public JwtTokenPair createTokenPair(User user) {
		long accessTokenExpiresIn = jwtProperties.accessTokenValiditySeconds();
		long refreshTokenExpiresIn = jwtProperties.refreshTokenValiditySeconds();

		return new JwtTokenPair(
			TOKEN_TYPE,
			createToken(user, accessTokenExpiresIn, ACCESS_TOKEN_USE),
			accessTokenExpiresIn,
			createToken(user, refreshTokenExpiresIn, REFRESH_TOKEN_USE),
			refreshTokenExpiresIn
		);
	}

	public Authentication getAuthentication(String token) {
		Claims claims = parseClaims(token);
		if (!ACCESS_TOKEN_USE.equals(claims.get("token_use", String.class))) {
			throw new BootSignalException(ErrorCode.UNAUTHORIZED, "Access Token만 사용할 수 있습니다.");
		}

		String email = resolveEmail(claims);
		String role = claims.get("role", String.class);
		if (!StringUtils.hasText(role)) {
			throw new BootSignalException(ErrorCode.UNAUTHORIZED, "토큰 권한 정보가 올바르지 않습니다.");
		}

		return new UsernamePasswordAuthenticationToken(
			email,
			token,
			List.of(new SimpleGrantedAuthority("ROLE_" + role))
		);
	}

	public JwtRefreshTokenClaims getRefreshTokenClaims(String token) {
		Claims claims = parseRefreshClaims(token);
		if (!REFRESH_TOKEN_USE.equals(claims.get("token_use", String.class))) {
			throw new BootSignalException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		Date expiration = claims.getExpiration();
		if (expiration == null) {
			throw new BootSignalException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

		return new JwtRefreshTokenClaims(resolveUserId(claims), expiration.toInstant());
	}

	private String createToken(User user, long validitySeconds, String tokenUse) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusSeconds(validitySeconds);

		return Jwts.builder()
			.issuer(jwtProperties.issuer())
			.subject(String.valueOf(user.getId()))
			.id(UUID.randomUUID().toString())
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiresAt))
			.claim("email", user.getEmail())
			.claim("nickname", user.getNickname())
			.claim("role", user.getRole().name())
			.claim("provider", user.getProvider().name())
			// 토큰 용도를 분리해 후속 검증에서 access/refresh를 구분한다.
			.claim("token_use", tokenUse)
			.signWith(signingKey())
			.compact();
	}

	private SecretKey signingKey() {
		return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}

	private Claims parseClaims(String token) {
		try {
			return Jwts.parser()
				.verifyWith(signingKey())
				.requireIssuer(jwtProperties.issuer())
				.build()
				.parseSignedClaims(token)
				.getPayload();
		} catch (ExpiredJwtException exception) {
			throw new BootSignalException(ErrorCode.UNAUTHORIZED, "만료된 인증 토큰입니다.");
		} catch (JwtException | IllegalArgumentException exception) {
			throw new BootSignalException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다.");
		}
	}

	private Claims parseRefreshClaims(String token) {
		try {
			return Jwts.parser()
				.verifyWith(signingKey())
				.requireIssuer(jwtProperties.issuer())
				.build()
				.parseSignedClaims(token)
				.getPayload();
		} catch (ExpiredJwtException exception) {
			throw new BootSignalException(ErrorCode.REFRESH_TOKEN_EXPIRED);
		} catch (JwtException | IllegalArgumentException exception) {
			throw new BootSignalException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
	}

	private String resolveEmail(Claims claims) {
		String email = EmailFormatValidator.normalize(claims.get("email", String.class));
		if (!EmailFormatValidator.isValid(email)) {
			throw new BootSignalException(ErrorCode.UNAUTHORIZED, "토큰 이메일 정보가 올바르지 않습니다.");
		}
		return email;
	}

	private Long resolveUserId(Claims claims) {
		try {
			return Long.valueOf(claims.getSubject());
		} catch (NumberFormatException exception) {
			throw new BootSignalException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
	}
}
