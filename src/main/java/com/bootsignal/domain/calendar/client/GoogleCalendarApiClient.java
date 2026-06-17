package com.bootsignal.domain.calendar.client;

import com.bootsignal.domain.calendar.client.dto.GoogleCalendarEventRequest;
import com.bootsignal.domain.calendar.client.dto.GoogleCalendarEventResponse;
import com.bootsignal.domain.calendar.client.dto.GoogleCalendarEventsListResponse;
import com.bootsignal.domain.calendar.client.dto.GoogleCalendarListedEvent;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

// Google Calendar 이벤트 관리 
@Slf4j
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

	/* 구글 Calendar 월별 이벤트 목록 조회 */
	public List<GoogleCalendarListedEvent> listEvents(String accessToken, String timeMin, String timeMax) {
		List<GoogleCalendarListedEvent> events = new ArrayList<>();
		String pageToken = null;

		try {
			do {
				GoogleCalendarEventsListResponse response = fetchEventsPage(accessToken, timeMin, timeMax, pageToken);
				if (response == null) {
					break;
				}

				if (response.items() != null) {
					events.addAll(response.items());
				}
				pageToken = response.nextPageToken();
			} while (StringUtils.hasText(pageToken));
		} catch (RestClientResponseException exception) {
			log.warn(
				"Google Calendar 일정 조회 API 실패. status={}, body={}",
				exception.getStatusCode(),
				exception.getResponseBodyAsString(),
				exception
			);
			throw new BootSignalException(
				ErrorCode.CALENDAR_FETCH_FAILED,
				resolveGoogleErrorMessage(exception)
			);
		} catch (RestClientException exception) {
			log.warn("Google Calendar 일정 조회 요청 실패.", exception);
			throw new BootSignalException(ErrorCode.CALENDAR_FETCH_FAILED);
		}

		return events;
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

	// 구글 Calendar 이벤트 목록 조회
	private GoogleCalendarEventsListResponse fetchEventsPage(
		String accessToken,
		String timeMin,
		String timeMax,
		String pageToken
	) {
		return restClientBuilder.build()
			.get()
			.uri(buildListEventsUri(timeMin, timeMax, pageToken))
			.headers(headers -> headers.setBearerAuth(accessToken))
			.retrieve()
			.body(GoogleCalendarEventsListResponse.class);
	}

	// 구글 Calendar 이벤트 목록 조회 URI 생성
	private URI buildListEventsUri(String timeMin, String timeMax, String pageToken) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(CALENDAR_EVENTS_URL)
			.queryParam("timeMin", timeMin)
			.queryParam("timeMax", timeMax)
			.queryParam("singleEvents", true)
			.queryParam("orderBy", "startTime")
			.queryParam("showDeleted", false);

		if (StringUtils.hasText(pageToken)) {
			builder.queryParam("pageToken", pageToken);
		}

		return builder.build().encode().toUri();
	}

	// 구글 캘린더 이벤트 목록 조회 실패 메시지 해석
	private String resolveGoogleErrorMessage(RestClientResponseException exception) {
		String responseBody = exception.getResponseBodyAsString();
		if (!StringUtils.hasText(responseBody)) {
			return ErrorCode.CALENDAR_FETCH_FAILED.message();
		}

		int messageStart = responseBody.indexOf("\"message\"");
		if (messageStart < 0) {
			return ErrorCode.CALENDAR_FETCH_FAILED.message();
		}

		int colonIndex = responseBody.indexOf(':', messageStart);
		int firstQuote = responseBody.indexOf('"', colonIndex + 1);
		int secondQuote = responseBody.indexOf('"', firstQuote + 1);
		if (firstQuote < 0 || secondQuote < 0) {
			return ErrorCode.CALENDAR_FETCH_FAILED.message();
		}

		return responseBody.substring(firstQuote + 1, secondQuote);
	}

	// 날짜 시간 형식 변환
	private String formatDateTime(LocalDateTime dateTime) {
		return dateTime.format(DATE_TIME_FORMATTER);
	}
}
