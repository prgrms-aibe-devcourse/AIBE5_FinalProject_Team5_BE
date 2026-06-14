package com.bootsignal.domain.calendar.client;

import com.bootsignal.domain.calendar.client.dto.GoogleCalendarEventRequest;
import com.bootsignal.domain.calendar.client.dto.GoogleCalendarEventResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Google 일정 생성 요청

@Component
@RequiredArgsConstructor
public class GoogleCalendarApiClient {

	private static final String CALENDAR_EVENTS_URL =
		"https://www.googleapis.com/calendar/v3/calendars/primary/events";
	private static final String TIME_ZONE = "Asia/Seoul";
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	private final RestClient.Builder restClientBuilder;

	/* 구글 Calendar 이벤트 생성 */
	public GoogleCalendarEventResponse createEvent(
		String accessToken,
		String summary,
		LocalDateTime startAt,
		LocalDateTime endAt
	) {
		GoogleCalendarEventRequest request = new GoogleCalendarEventRequest(
			summary,
			new GoogleCalendarEventRequest.EventDateTime(formatDateTime(startAt), TIME_ZONE),
			new GoogleCalendarEventRequest.EventDateTime(formatDateTime(endAt), TIME_ZONE)
		);

		return restClientBuilder.build()
			.post()
			.uri(CALENDAR_EVENTS_URL)
			.headers(headers -> headers.setBearerAuth(accessToken))
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.retrieve()
			.body(GoogleCalendarEventResponse.class);
	}

	/* 구글 Calendar 이벤트 삭제 */
	public void deleteEvent(String accessToken, String googleEventId) {
		restClientBuilder.build()
			.delete()
			.uri(CALENDAR_EVENTS_URL + "/" + googleEventId)
			.headers(headers -> headers.setBearerAuth(accessToken))
			.retrieve()
			.toBodilessEntity();
	}

	/* 날짜 시간 포맷 변환 */
	private String formatDateTime(LocalDateTime dateTime) {
		return dateTime.format(DATE_TIME_FORMATTER);
	}
}
