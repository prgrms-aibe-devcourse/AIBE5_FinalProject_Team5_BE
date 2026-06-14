package com.bootsignal.domain.calendar.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// 구글 캘린더 OAuth 토큰 응답 데이터 양식
public record GoogleOAuthTokenResponse(
	@JsonProperty("access_token")
	String accessToken,
	@JsonProperty("expires_in")
	long expiresIn,
	@JsonProperty("refresh_token")
	String refreshToken,
	@JsonProperty("scope")
	String scope,
	@JsonProperty("token_type")
	String tokenType
) {
}
