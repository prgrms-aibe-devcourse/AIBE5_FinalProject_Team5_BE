package com.bootsignal.domain.calendar.controller;

import com.bootsignal.domain.calendar.dto.CalendarStatusResponse;
import com.bootsignal.domain.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
