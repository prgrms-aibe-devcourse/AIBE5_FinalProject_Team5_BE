package com.bootsignal.domain.calendar.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Google Calendar 이벤트 목록 응답용 단일 이벤트 데이터 양식
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleCalendarListedEvent(
	String id,
	String status,
	String summary, // 이벤트 제목
	String description,
	EventDateTime start,
	EventDateTime end
) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record EventDateTime(
		String dateTime,
		String date,
		String timeZone
	) {
	}
}
