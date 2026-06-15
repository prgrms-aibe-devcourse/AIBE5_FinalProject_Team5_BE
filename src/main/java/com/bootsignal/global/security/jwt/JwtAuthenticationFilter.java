package com.bootsignal.global.security.jwt;

import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Authorization 헤더의 Access Token을 검증해 SecurityContext에 인증 정보를 설정하는 필터입니다.
 * 인증 API는 Refresh Token 본문 검증 흐름과 충돌하지 않도록 필터 대상에서 제외합니다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String AUTH_API_PREFIX = "/api/auth/";
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;
	private final HandlerExceptionResolver handlerExceptionResolver;

	public JwtAuthenticationFilter(
		JwtTokenProvider jwtTokenProvider,
		HandlerExceptionResolver handlerExceptionResolver
	) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.handlerExceptionResolver = handlerExceptionResolver;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String requestPath = request.getRequestURI();
		String contextPath = request.getContextPath();
		if (StringUtils.hasText(contextPath) && requestPath.startsWith(contextPath)) {
			requestPath = requestPath.substring(contextPath.length());
		}
		return requestPath.startsWith(AUTH_API_PREFIX);
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		try {
			String token = resolveToken(request);
			if (StringUtils.hasText(token)) {
				Authentication authentication = jwtTokenProvider.getAuthentication(token);
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
			filterChain.doFilter(request, response);
		} catch (BootSignalException exception) {
			// 필터 단계의 인증 오류도 공통 예외 응답 형식으로 변환한다.
			SecurityContextHolder.clearContext();
			handlerExceptionResolver.resolveException(request, response, null, exception);
		}
	}

	private String resolveToken(HttpServletRequest request) {
		String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (!StringUtils.hasText(authorizationHeader)) {
			return null;
		}
		if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new BootSignalException(ErrorCode.UNAUTHORIZED, "Bearer 인증 토큰이 필요합니다.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length()).strip();
	}
}
