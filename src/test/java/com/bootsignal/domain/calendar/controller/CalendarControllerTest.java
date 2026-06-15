package com.bootsignal.domain.calendar.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.calendar.dto.CalendarEventItemResponse;
import com.bootsignal.domain.calendar.dto.CalendarEventListResponse;
import com.bootsignal.domain.calendar.dto.CalendarStatusResponse;
import com.bootsignal.domain.calendar.entity.CalendarEventType;
import com.bootsignal.domain.calendar.service.CalendarEventQueryService;
import com.bootsignal.domain.calendar.service.CalendarService;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CalendarController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CalendarController 테스트")
class CalendarControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CalendarService calendarService;

	@MockitoBean
	private CalendarEventQueryService calendarEventQueryService;

	@Test
	@DisplayName("GET /api/calendar/events — 연동 사용자 일정 목록 (200)")
	void getEventsReturnsMonthlyEvents() throws Exception {
		given(calendarEventQueryService.getMonthlyEvents(2026, 6))
			.willReturn(new CalendarEventListResponse(
				2026,
				6,
				true,
				true,
				2,
				List.of(
					new CalendarEventItemResponse(
						101L,
						"abc123google",
						"[과정 시작] Spring Boot 실무",
						"BootSignal에서 자동 등록된 과정 시작 일정",
						LocalDateTime.of(2026, 6, 1, 0, 0),
						LocalDateTime.of(2026, 6, 1, 23, 59, 59),
						CalendarEventType.COURSE_START,
						34L
					),
					new CalendarEventItemResponse(
						null,
						"xyz789google",
						"팀 미팅",
						"주간 스탠드업",
						LocalDateTime.of(2026, 6, 10, 14, 0),
						LocalDateTime.of(2026, 6, 10, 15, 0),
						CalendarEventType.CUSTOM,
						null
					)
				)
			));

		mockMvc.perform(get("/api/calendar/events")
				.param("year", "2026")
				.param("month", "6"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.year").value(2026))
			.andExpect(jsonPath("$.data.month").value(6))
			.andExpect(jsonPath("$.data.googleUser").value(true))
			.andExpect(jsonPath("$.data.calendarConnected").value(true))
			.andExpect(jsonPath("$.data.totalCount").value(2))
			.andExpect(jsonPath("$.data.events[0].id").value(101))
			.andExpect(jsonPath("$.data.events[0].eventType").value("COURSE_START"))
			.andExpect(jsonPath("$.data.events[0].courseSessionId").value(34))
			.andExpect(jsonPath("$.data.events[1].id").doesNotExist())
			.andExpect(jsonPath("$.data.events[1].eventType").value("CUSTOM"))
			.andExpect(jsonPath("$.data.events[1].courseSessionId").doesNotExist());
	}

	@Test
	@DisplayName("GET /api/calendar/events — 미연동 사용자 북마크 일정 (200)")
	void getEventsReturnsBookmarkEventsWhenNotConnected() throws Exception {
		given(calendarEventQueryService.getMonthlyEvents(2026, 9))
			.willReturn(new CalendarEventListResponse(
				2026,
				9,
				true,
				false,
				1,
				List.of(
					new CalendarEventItemResponse(
						3L,
						"66erb1l4lfkbhk05th02s7idtc",
						"[과정 시작] Spring Boot 실무",
						null,
						LocalDateTime.of(2026, 9, 11, 0, 0),
						LocalDateTime.of(2026, 9, 11, 23, 59, 59),
						CalendarEventType.COURSE_START,
						1L
					)
				)
			));

		mockMvc.perform(get("/api/calendar/events")
				.param("year", "2026")
				.param("month", "9"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.calendarConnected").value(false))
			.andExpect(jsonPath("$.data.totalCount").value(1))
			.andExpect(jsonPath("$.data.events[0].eventType").value("COURSE_START"))
			.andExpect(jsonPath("$.data.events[0].id").value(3))
			.andExpect(jsonPath("$.data.events[0].googleEventId").value("66erb1l4lfkbhk05th02s7idtc"));
	}

	@Test
	@DisplayName("GET /api/calendar/events — 미연동 사용자 빈 목록 (200)")
	void getEventsReturnsEmptyListWhenNotConnected() throws Exception {
		given(calendarEventQueryService.getMonthlyEvents(2026, 6))
			.willReturn(CalendarEventListResponse.empty(2026, 6, true, false));

		mockMvc.perform(get("/api/calendar/events")
				.param("year", "2026")
				.param("month", "6"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.calendarConnected").value(false))
			.andExpect(jsonPath("$.data.totalCount").value(0))
			.andExpect(jsonPath("$.data.events").isEmpty());
	}

	@Test
	@DisplayName("GET /api/calendar/events — month 파라미터 누락 (400)")
	void getEventsReturnsBadRequestWhenMonthMissing() throws Exception {
		mockMvc.perform(get("/api/calendar/events")
				.param("year", "2026"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
			.andExpect(jsonPath("$.error.message").value("필수 요청 파라미터 'month'가 없습니다."));
	}

	@Test
	@DisplayName("GET /api/calendar/events — Google 조회 실패 (502)")
	void getEventsReturnsBadGatewayWhenGoogleFetchFails() throws Exception {
		given(calendarEventQueryService.getMonthlyEvents(2026, 6))
			.willThrow(new BootSignalException(ErrorCode.CALENDAR_FETCH_FAILED));

		mockMvc.perform(get("/api/calendar/events")
				.param("year", "2026")
				.param("month", "6"))
			.andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("CALENDAR_FETCH_FAILED"));
	}

	@Test
	@DisplayName("GET /api/calendar/status — 연동됨 (200)")
	void getStatusReturnsConnected() throws Exception {
		// given
		given(calendarService.getStatus())
			.willReturn(CalendarStatusResponse.connected(
				LocalDateTime.of(2026, 6, 11, 10, 0),
				LocalDateTime.of(2026, 6, 11, 12, 0)
			));

		// when & then
		mockMvc.perform(get("/api/calendar/status"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.connected").value(true))
			.andExpect(jsonPath("$.data.googleUser").value(true))
			.andExpect(jsonPath("$.data.connectedAt").exists())
			.andExpect(jsonPath("$.data.expiresAt").exists())
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	@DisplayName("GET /api/calendar/status — 구글 사용자 미연동 (200)")
	void getStatusReturnsNotConnected() throws Exception {
		// given
		given(calendarService.getStatus())
			.willReturn(CalendarStatusResponse.notConnected());

		// when & then
		mockMvc.perform(get("/api/calendar/status"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.connected").value(false))
			.andExpect(jsonPath("$.data.googleUser").value(true))
			.andExpect(jsonPath("$.data.connectedAt").doesNotExist())
			.andExpect(jsonPath("$.data.expiresAt").doesNotExist());
	}

	@Test
	@DisplayName("GET /api/calendar/status — 비구글 사용자 (200)")
	void getStatusReturnsNotGoogleUser() throws Exception {
		// given
		given(calendarService.getStatus())
			.willReturn(CalendarStatusResponse.notGoogleUser());

		// when & then
		mockMvc.perform(get("/api/calendar/status"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.connected").value(false))
			.andExpect(jsonPath("$.data.googleUser").value(false));
	}

	@Test
	@DisplayName("GET /api/calendar/status — 미로그인 (401)")
	void getStatusReturnsUnauthorized() throws Exception {
		// given
		given(calendarService.getStatus())
			.willThrow(new BootSignalException(ErrorCode.UNAUTHORIZED));

		// when & then
		mockMvc.perform(get("/api/calendar/status"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("GET /api/calendar/connect/google — Google OAuth redirect (302)")
	void startGoogleConnectRedirectsToGoogle() throws Exception {
		// given
		given(calendarService.startGoogleConnect())
			.willReturn("https://accounts.google.com/o/oauth2/v2/auth?state=test");

		// when & then
		mockMvc.perform(get("/api/calendar/connect/google"))
			.andExpect(status().isFound())
			.andExpect(header().string("Location", "https://accounts.google.com/o/oauth2/v2/auth?state=test"));
	}

	@Test
	@DisplayName("GET /api/calendar/connect/google/callback — 성공 시 success URL redirect (302)")
	void completeGoogleConnectRedirectsToSuccessUrl() throws Exception {
		// given
		given(calendarService.completeGoogleConnect("auth-code", "oauth-state"))
			.willReturn(CalendarStatusResponse.connected(
				LocalDateTime.of(2026, 6, 14, 14, 0),
				LocalDateTime.of(2026, 6, 14, 15, 0)
			));
		given(calendarService.connectSuccessRedirectUrl())
			.willReturn("http://localhost:5173/dashboard/schedule");

		// when & then
		mockMvc.perform(get("/api/calendar/connect/google/callback")
				.param("code", "auth-code")
				.param("state", "oauth-state"))
			.andExpect(status().isFound())
			.andExpect(header().string("Location", "http://localhost:5173/dashboard/schedule"));
	}

	@Test
	@DisplayName("GET /api/calendar/connect/google/callback — Google error redirect (302)")
	void completeGoogleConnectRedirectsToFailureUrlWhenGoogleReturnsError() throws Exception {
		// given
		given(calendarService.connectFailureRedirectUrl())
			.willReturn("http://localhost:3000/calendar/connect/failure");

		// when & then
		mockMvc.perform(get("/api/calendar/connect/google/callback")
				.param("error", "access_denied"))
			.andExpect(status().isFound())
			.andExpect(header().string("Location", "http://localhost:3000/calendar/connect/failure?error=access_denied"));
	}

	@Test
	@DisplayName("DELETE /api/calendar/disconnect — 연동 해제 후 상태 반환 (200)")
	void disconnectGoogleReturnsDisconnectedStatus() throws Exception {
		// given
		given(calendarService.disconnectGoogle())
			.willReturn(CalendarStatusResponse.notConnected());

		// when & then
		mockMvc.perform(delete("/api/calendar/disconnect"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.connected").value(false))
			.andExpect(jsonPath("$.data.googleUser").value(true))
			.andExpect(jsonPath("$.data.connectedAt").doesNotExist())
			.andExpect(jsonPath("$.data.expiresAt").doesNotExist());
	}

	@Test
	@DisplayName("DELETE /api/calendar/disconnect — 미연동 (400)")
	void disconnectGoogleReturnsBadRequestWhenNotConnected() throws Exception {
		// given
		doThrow(new BootSignalException(ErrorCode.CALENDAR_NOT_CONNECTED))
			.when(calendarService).disconnectGoogle();

		// when & then
		mockMvc.perform(delete("/api/calendar/disconnect"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("CALENDAR_NOT_CONNECTED"));
	}
}
