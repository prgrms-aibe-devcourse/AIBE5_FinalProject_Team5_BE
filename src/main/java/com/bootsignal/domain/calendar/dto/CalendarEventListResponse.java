package com.bootsignal.domain.calendar.dto;

import java.util.List;

// Google Calendar 이벤트 목록 응답 데이터 양식
public record CalendarEventListResponse(
	int year,
	int month,
	boolean googleUser,
	boolean calendarConnected,
	int totalCount,
	List<CalendarEventItemResponse> events
) {

	public static CalendarEventListResponse empty(int year, int month, boolean googleUser, boolean calendarConnected) {
		return new CalendarEventListResponse(year, month, googleUser, calendarConnected, 0, List.of());
	}
}
