package com.bootsignal.global.security.jwt;

import com.bootsignal.domain.auth.dto.LoginResponse;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * JWT 인증 쿠키와 CSRF 토큰 쿠키를 발급, 삭제, 추출하는 보안 유틸리티입니다.
 */
@Component
public class JwtTokenCookieManager {

	public static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
	public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
	public static final String CSRF_TOKEN_COOKIE_NAME = "XSRF-TOKEN";
	public static final String CSRF_TOKEN_HEADER_NAME = "X-XSRF-TOKEN";

	private static final String ACCESS_TOKEN_COOKIE_PATH = "/";
	private static final String REFRESH_TOKEN_COOKIE_PATH = "/api/auth";
	private static final String LEGACY_REFRESH_TOKEN_COOKIE_PATH = "/";
	private static final String CSRF_TOKEN_COOKIE_PATH = "/";
	private static final int CSRF_TOKEN_BYTES = 32;

	private final boolean cookieSecure;
	private final String cookieSameSite;
	private final String cookieDomain;
	private final SecureRandom secureRandom = new SecureRandom();

	public JwtTokenCookieManager(
		@Value("${app.security.jwt.cookie.secure:false}") boolean cookieSecure,
		@Value("${app.security.jwt.cookie.same-site:Lax}") String cookieSameSite,
		@Value("${app.security.jwt.cookie.domain:}") String cookieDomain
	) {
		this.cookieSecure = cookieSecure;
		this.cookieSameSite = StringUtils.hasText(cookieSameSite) ? cookieSameSite : "Lax";
		this.cookieDomain = StringUtils.hasText(cookieDomain) ? cookieDomain : null;
	}

	public void addTokenCookies(HttpServletResponse response, LoginResponse loginResponse) {
		addCookie(
			response,
			ACCESS_TOKEN_COOKIE_NAME,
			loginResponse.accessToken(),
			loginResponse.accessTokenExpiresIn(),
			ACCESS_TOKEN_COOKIE_PATH,
			true
		);
		addCookie(
			response,
			REFRESH_TOKEN_COOKIE_NAME,
			loginResponse.refreshToken(),
			loginResponse.refreshTokenExpiresIn(),
			REFRESH_TOKEN_COOKIE_PATH,
			true
		);
		// 이전 구현에서 Path=/로 발급된 refreshToken 쿠키가 남아 있으면 더 좁은 경로 쿠키와 충돌할 수 있어 제거한다.
		addExpiredCookie(response, REFRESH_TOKEN_COOKIE_NAME, LEGACY_REFRESH_TOKEN_COOKIE_PATH, true);
		addCookie(
			response,
			CSRF_TOKEN_COOKIE_NAME,
			generateCsrfToken(),
			loginResponse.refreshTokenExpiresIn(),
			CSRF_TOKEN_COOKIE_PATH,
			false
		);
	}

	public void clearTokenCookies(HttpServletResponse response) {
		addExpiredCookie(response, ACCESS_TOKEN_COOKIE_NAME, ACCESS_TOKEN_COOKIE_PATH, true);
		addExpiredCookie(response, REFRESH_TOKEN_COOKIE_NAME, REFRESH_TOKEN_COOKIE_PATH, true);
		addExpiredCookie(response, REFRESH_TOKEN_COOKIE_NAME, LEGACY_REFRESH_TOKEN_COOKIE_PATH, true);
		addExpiredCookie(response, CSRF_TOKEN_COOKIE_NAME, CSRF_TOKEN_COOKIE_PATH, false);
	}

	public String resolveAccessToken(HttpServletRequest request) {
		return resolveCookieValue(request, ACCESS_TOKEN_COOKIE_NAME);
	}

	public String requireRefreshToken(HttpServletRequest request) {
		String refreshToken = resolveCookieValue(request, REFRESH_TOKEN_COOKIE_NAME);
		if (!StringUtils.hasText(refreshToken)) {
			throw new BootSignalException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		return refreshToken;
	}

	public String resolveCsrfToken(HttpServletRequest request) {
		return resolveCookieValue(request, CSRF_TOKEN_COOKIE_NAME);
	}

	public boolean hasAuthCookie(HttpServletRequest request) {
		return StringUtils.hasText(resolveAccessToken(request))
			|| StringUtils.hasText(resolveCookieValue(request, REFRESH_TOKEN_COOKIE_NAME));
	}

	private void addCookie(
		HttpServletResponse response,
		String name,
		String value,
		long maxAgeSeconds,
		String path,
		boolean httpOnly
	) {
		ResponseCookie cookie = baseCookie(name, value, path, httpOnly)
			.maxAge(Duration.ofSeconds(maxAgeSeconds))
			.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	private void addExpiredCookie(HttpServletResponse response, String name, String path, boolean httpOnly) {
		ResponseCookie cookie = baseCookie(name, "", path, httpOnly)
			.maxAge(Duration.ZERO)
			.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	private ResponseCookie.ResponseCookieBuilder baseCookie(String name, String value, String path, boolean httpOnly) {
		ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
			.httpOnly(httpOnly)
			.secure(cookieSecure)
			.path(path)
			.sameSite(cookieSameSite);
		if (cookieDomain != null) {
			builder = builder.domain(cookieDomain);
		}
		return builder;
	}

	private String generateCsrfToken() {
		byte[] randomBytes = new byte[CSRF_TOKEN_BYTES];
		secureRandom.nextBytes(randomBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

	private String resolveCookieValue(HttpServletRequest request, String cookieName) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		return Arrays.stream(cookies)
			.filter(cookie -> cookieName.equals(cookie.getName()))
			.map(Cookie::getValue)
			.filter(StringUtils::hasText)
			.findFirst()
			.orElse(null);
	}
}
