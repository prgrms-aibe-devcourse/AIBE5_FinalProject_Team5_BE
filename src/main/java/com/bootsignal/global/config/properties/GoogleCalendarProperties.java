package com.bootsignal.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.calendar.google")
public record GoogleCalendarProperties(
	String redirectUri,
	String scope,
	String connectSuccessUrl,
	String connectFailureUrl
) {

	public GoogleCalendarProperties {
		// 기본값: 구글 캘린더 연동 권한 범위 설정
		scope = (scope == null || scope.isBlank())
			? "openid email profile https://www.googleapis.com/auth/calendar.events"
			: scope;
		
		// 기본값: 프론트 Dashboard 스케줄 페이지
		connectSuccessUrl = (connectSuccessUrl == null || connectSuccessUrl.isBlank())

			? "http://localhost:5173/dashboard/schedule"
		// 기본값: 프론트 Dashboard 스케줄 페이지
			: connectSuccessUrl;
		connectFailureUrl = (connectFailureUrl == null || connectFailureUrl.isBlank())
			? "http://localhost:5173/dashboard/schedule"
			: connectFailureUrl;
	}
}
