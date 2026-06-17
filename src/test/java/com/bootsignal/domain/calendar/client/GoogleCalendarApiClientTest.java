package com.bootsignal.domain.calendar.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("GoogleCalendarApiClient 테스트")
class GoogleCalendarApiClientTest {

	private MockRestServiceServer mockServer;
	private GoogleCalendarApiClient googleCalendarApiClient;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		mockServer = MockRestServiceServer.bindTo(builder).build();
		googleCalendarApiClient = new GoogleCalendarApiClient(builder);
	}

	@Test
	@DisplayName("listEvents — UTC timeMin/timeMax로 Google API 호출")
	void listEventsCallsGoogleApiWithUtcTimeRange() {
		mockServer.expect(requestTo(
				"https://www.googleapis.com/calendar/v3/calendars/primary/events"
					+ "?timeMin=2026-08-31T15:00:00Z"
					+ "&timeMax=2026-09-30T15:00:00Z"
					+ "&singleEvents=true&orderBy=startTime&showDeleted=false"
			))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("Authorization", "Bearer access-token"))
			.andRespond(withSuccess("""
				{
				  "items": [
				    {
				      "id": "event-1",
				      "status": "confirmed",
				      "summary": "팀 미팅",
				      "description": "주간 스탠드업",
				      "start": { "dateTime": "2026-09-10T14:00:00+09:00", "timeZone": "Asia/Seoul" },
				      "end": { "dateTime": "2026-09-10T15:00:00+09:00", "timeZone": "Asia/Seoul" }
				    }
				  ]
				}
				""", MediaType.APPLICATION_JSON));

		List<?> events = googleCalendarApiClient.listEvents(
			"access-token",
			"2026-08-31T15:00:00Z",
			"2026-09-30T15:00:00Z"
		);

		assertThat(events).hasSize(1);
		mockServer.verify();
	}

	@Test
	@DisplayName("listEvents — Google 403 응답 시 CALENDAR_FETCH_FAILED")
	void listEventsThrowsWhenGoogleReturnsForbidden() {
		mockServer.expect(requestTo(Matchers.containsString("/calendar/v3/calendars/primary/events")))
			.andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest()
				.body("""
					{
					  "error": {
					    "code": 403,
					    "message": "Request had insufficient authentication scopes."
					  }
					}
					"""));

		assertThatThrownBy(() -> googleCalendarApiClient.listEvents(
			"access-token",
			"2026-08-31T15:00:00Z",
			"2026-09-30T15:00:00Z"
		))
			.isInstanceOf(BootSignalException.class)
			.satisfies(exception -> {
				BootSignalException bootSignalException = (BootSignalException) exception;
				assertThat(bootSignalException.errorCode()).isEqualTo(ErrorCode.CALENDAR_FETCH_FAILED);
				assertThat(bootSignalException.getMessage())
					.isEqualTo("Request had insufficient authentication scopes.");
			});
	}
}
