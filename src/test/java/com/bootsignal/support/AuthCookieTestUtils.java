package com.bootsignal.support;

import com.bootsignal.global.security.jwt.JwtTokenCookieManager;
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 인증 통합 테스트에서 Set-Cookie 헤더의 JWT 쿠키 값을 추출하는 테스트 전용 유틸리티입니다.
 */
public final class AuthCookieTestUtils {

	private AuthCookieTestUtils() {
	}

	public static String extractAccessToken(MvcResult result) {
		return extractCookieValue(result, JwtTokenCookieManager.ACCESS_TOKEN_COOKIE_NAME);
	}

	public static String extractRefreshToken(MvcResult result) {
		return extractCookieValue(result, JwtTokenCookieManager.REFRESH_TOKEN_COOKIE_NAME);
	}

	public static String extractCsrfToken(MvcResult result) {
		return extractCookieValue(result, JwtTokenCookieManager.CSRF_TOKEN_COOKIE_NAME);
	}

	public static Cookie extractAccessTokenCookie(MvcResult result) {
		return new Cookie(JwtTokenCookieManager.ACCESS_TOKEN_COOKIE_NAME, extractAccessToken(result));
	}

	public static Cookie extractRefreshTokenCookie(MvcResult result) {
		return new Cookie(JwtTokenCookieManager.REFRESH_TOKEN_COOKIE_NAME, extractRefreshToken(result));
	}

	public static Cookie extractCsrfTokenCookie(MvcResult result) {
		return new Cookie(JwtTokenCookieManager.CSRF_TOKEN_COOKIE_NAME, extractCsrfToken(result));
	}

	private static String extractCookieValue(MvcResult result, String cookieName) {
		String cookieHeader = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
			.filter(header -> header.startsWith(cookieName + "="))
			.findFirst()
			.orElseThrow(() -> new AssertionError(cookieName + " 쿠키가 응답에 없습니다."));
		int valueEndIndex = cookieHeader.indexOf(';');
		if (valueEndIndex < 0) {
			return cookieHeader.substring(cookieName.length() + 1);
		}
		return cookieHeader.substring(cookieName.length() + 1, valueEndIndex);
	}
}
