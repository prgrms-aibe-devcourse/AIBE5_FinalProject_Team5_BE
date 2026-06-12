package com.bootsignal.domain.calendar.dto;

import java.time.LocalDateTime;

public record CalendarStatusResponse(
	boolean connected,
	boolean googleUser,
	LocalDateTime connectedAt,
	LocalDateTime expiresAt
) {

	/* 구글 사용자 아님 */
	public static CalendarStatusResponse notGoogleUser() {
		return new CalendarStatusResponse(false, false, null, null);
	}

	/* 구글 캘린더 연동 안된 구글 사용자 */
	public static CalendarStatusResponse notConnected() {
		return new CalendarStatusResponse(false, true, null, null);
	}

	/* 구글 캘린더 연동 된 구글 사용자 */
	public static CalendarStatusResponse connected(LocalDateTime connectedAt, LocalDateTime expiresAt) {
		return new CalendarStatusResponse(true, true, connectedAt, expiresAt);
	}
}
