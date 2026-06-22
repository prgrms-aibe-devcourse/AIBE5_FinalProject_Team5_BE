package com.bootsignal.domain.calendar.dto;

public record CalendarConnectResponse(
	String redirectUrl
) {

	public static CalendarConnectResponse of(String redirectUrl) {
		return new CalendarConnectResponse(redirectUrl);
	}
}
