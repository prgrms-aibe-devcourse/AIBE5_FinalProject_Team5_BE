package com.bootsignal.domain.calendar.controller;

import com.bootsignal.domain.calendar.dto.CalendarConnectResponse;
import com.bootsignal.domain.calendar.dto.CalendarEventListResponse;
import com.bootsignal.domain.calendar.dto.CalendarStatusResponse;
import com.bootsignal.domain.calendar.service.CalendarEventQueryService;
import com.bootsignal.domain.calendar.service.CalendarService;
import com.bootsignal.global.exception.BootSignalException;
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
	private final CalendarEventQueryService calendarEventQueryService;

	/* 구글 캘린더 연동 상태 조회 */
	@GetMapping("/status")
	public CalendarStatusResponse getStatus() {
		return calendarService.getStatus();
	}

	/* Google Calendar OAuth 연동 시작 (redirect URL JSON 반환) */
	@GetMapping("/connect/google")
	public CalendarConnectResponse startGoogleConnect() {
		String authorizationUrl = calendarService.startGoogleConnect();
		return CalendarConnectResponse.of(authorizationUrl);
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

	/* 월별 일정 목록 조회 */
	@GetMapping("/events")
	public CalendarEventListResponse getEvents(
		@RequestParam int year,
		@RequestParam int month
	) {
		return calendarEventQueryService.getMonthlyEvents(year, month);
	}


	// 구글 캘린더 연동 성공 프론트 리다이렉트 
	private ResponseEntity<Void> redirectToSuccess() {
		return ResponseEntity.status(HttpStatus.FOUND)
			.location(URI.create(calendarService.connectSuccessRedirectUrl()))
			.build();
	}

	// 구글 캘린더 연동 실패 프론트 리다이렉트
	private ResponseEntity<Void> redirectToFailure(String message) {
		String failureUrl = calendarService.connectFailureRedirectUrl()
			+ "?error=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
		return ResponseEntity.status(HttpStatus.FOUND)
			.location(URI.create(failureUrl))
			.build();
	}

}
