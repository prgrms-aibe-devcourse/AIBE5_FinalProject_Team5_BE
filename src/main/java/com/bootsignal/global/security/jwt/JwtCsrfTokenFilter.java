package com.bootsignal.global.security.jwt;

import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 쿠키 기반 JWT 인증 요청의 CSRF 토큰을 검증해 브라우저 자동 쿠키 전송 공격을 차단하는 필터입니다.
 */
public class JwtCsrfTokenFilter extends OncePerRequestFilter {

	private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
	private static final String AUTH_API_PREFIX = "/api/auth/";
	private static final String REFRESH_API_PATH = "/api/auth/refresh";
	private static final String LOGOUT_API_PATH = "/api/auth/logout";

	private final JwtTokenCookieManager jwtTokenCookieManager;
	private final HandlerExceptionResolver handlerExceptionResolver;

	public JwtCsrfTokenFilter(
		JwtTokenCookieManager jwtTokenCookieManager,
		HandlerExceptionResolver handlerExceptionResolver
	) {
		this.jwtTokenCookieManager = jwtTokenCookieManager;
		this.handlerExceptionResolver = handlerExceptionResolver;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		try {
			validateCsrfTokenIfNeeded(request);
			filterChain.doFilter(request, response);
		} catch (BootSignalException exception) {
			handlerExceptionResolver.resolveException(request, response, null, exception);
		}
	}

	private void validateCsrfTokenIfNeeded(HttpServletRequest request) {
		if (SAFE_METHODS.contains(request.getMethod())) {
			return;
		}
		if (StringUtils.hasText(request.getHeader(HttpHeaders.AUTHORIZATION))) {
			return;
		}
		if (isPublicAuthApi(request)) {
			return;
		}
		if (!jwtTokenCookieManager.hasAuthCookie(request)) {
			return;
		}

		String csrfCookie = jwtTokenCookieManager.resolveCsrfToken(request);
		String csrfHeader = request.getHeader(JwtTokenCookieManager.CSRF_TOKEN_HEADER_NAME);
		if (!StringUtils.hasText(csrfCookie) || !matches(csrfCookie, csrfHeader)) {
			throw new BootSignalException(ErrorCode.CSRF_TOKEN_INVALID);
		}
	}

	private boolean isPublicAuthApi(HttpServletRequest request) {
		String requestPath = request.getRequestURI();
		String contextPath = request.getContextPath();
		if (StringUtils.hasText(contextPath) && requestPath.startsWith(contextPath)) {
			requestPath = requestPath.substring(contextPath.length());
		}
		return requestPath.startsWith(AUTH_API_PREFIX)
			&& !REFRESH_API_PATH.equals(requestPath)
			&& !LOGOUT_API_PATH.equals(requestPath);
	}

	private boolean matches(String expected, String actual) {
		if (!StringUtils.hasText(actual)) {
			return false;
		}
		return MessageDigest.isEqual(
			expected.getBytes(StandardCharsets.UTF_8),
			actual.getBytes(StandardCharsets.UTF_8)
		);
	}
}
