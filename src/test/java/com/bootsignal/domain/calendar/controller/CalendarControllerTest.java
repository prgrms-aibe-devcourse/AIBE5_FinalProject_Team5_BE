package com.bootsignal.domain.calendar.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
