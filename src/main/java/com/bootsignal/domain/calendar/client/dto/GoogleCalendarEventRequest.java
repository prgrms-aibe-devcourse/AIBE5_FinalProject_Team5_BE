package com.bootsignal.domain.calendar.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

// 구글 Calendar 이벤트 요청 데이터 양식
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoogleCalendarEventRequest(
	String summary,
	EventDateTime start,
	EventDateTime end
) {
	public record EventDateTime(
		String dateTime,
		String timeZone
	) {
	}
}
