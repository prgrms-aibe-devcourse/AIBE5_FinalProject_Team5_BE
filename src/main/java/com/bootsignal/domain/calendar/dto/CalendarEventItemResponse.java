package com.bootsignal.domain.calendar.dto;

import com.bootsignal.domain.calendar.entity.CalendarEventType;
import java.time.LocalDateTime;

// Google Calendar 이벤트 목록 응답용 단일 이벤트 데이터 양식
public record CalendarEventItemResponse(
	Long id,
	String googleEventId,
	String title,
	String description,
	LocalDateTime startAt,
	LocalDateTime endAt,
	CalendarEventType eventType,
	Long courseSessionId
) {
}
