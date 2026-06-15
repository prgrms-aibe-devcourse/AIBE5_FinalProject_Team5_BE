package com.bootsignal.domain.calendar.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// Google Calendar 이벤트 목록 응답 데이터 양식
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleCalendarEventsListResponse(
	List<GoogleCalendarListedEvent> items,
	String nextPageToken
) {
}
