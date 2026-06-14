package com.bootsignal.domain.calendar.controller;

import com.bootsignal.domain.calendar.dto.CalendarStatusResponse;
import com.bootsignal.domain.calendar.service.CalendarService;
import com.bootsignal.global.exception.BootSignalException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

	private final CalendarService calendarService;

	/* 구글 캘린더 연동 상태 조회 */
	@GetMapping("/status")
	public CalendarStatusResponse getStatus() {
		return calendarService.getStatus();
	}

	/* Google Calendar OAuth 연동 시작 */
	@GetMapping("/connect/google")
	public void startGoogleConnect(HttpServletResponse response) throws IOException {
		// 구글 캘린더 OAuth 인증 URL 생성
		String authorizationUrl = calendarService.startGoogleConnect();
		
		// 리다이렉트 응답 전송
		response.sendRedirect(authorizationUrl);
	}

	/* Google Calendar OAuth callback (연동 -> 구글 -> 콜백) */
	@GetMapping("/connect/google/callback")
	public ResponseEntity<Void> completeGoogleConnect(
		@RequestParam(required = false) String code,
		@RequestParam(required = false) String state,
		@RequestParam(required = false) String error
	) {
		if (StringUtils.hasText(error)) {
			return redirectToFailure(error);
		}
		if (!StringUtils.hasText(code) || !StringUtils.hasText(state)) {
			return redirectToFailure("missing_code_or_state");
		}

		try {
			calendarService.completeGoogleConnect(code, state);
			return redirectToSuccess();
		} catch (BootSignalException exception) {
			return redirectToFailure(exception.getMessage());
		}
	}

	/* Google Calendar 연동 해제 */
	@DeleteMapping("/disconnect")
	public CalendarStatusResponse disconnectGoogle() {
		return calendarService.disconnectGoogle();
	}


	private ResponseEntity<Void> redirectToSuccess() {
		return ResponseEntity.status(HttpStatus.FOUND)
			.location(URI.create(calendarService.connectSuccessRedirectUrl()))
			.build();
	}

	private ResponseEntity<Void> redirectToFailure(String message) {
		String failureUrl = calendarService.connectFailureRedirectUrl()
			+ "?error=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
		return ResponseEntity.status(HttpStatus.FOUND)
			.location(URI.create(failureUrl))
			.build();
	}

}
