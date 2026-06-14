package com.bootsignal.domain.calendar.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.calendar.dto.CalendarStatusResponse;
import com.bootsignal.domain.calendar.service.CalendarService;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.time.LocalDateTime;
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
