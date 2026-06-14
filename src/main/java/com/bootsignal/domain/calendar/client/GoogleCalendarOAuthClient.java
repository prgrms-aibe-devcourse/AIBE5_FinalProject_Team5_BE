package com.bootsignal.domain.calendar.client;

import com.bootsignal.domain.calendar.client.dto.GoogleOAuthTokenResponse;
import com.bootsignal.global.config.properties.GoogleCalendarProperties;
import com.bootsignal.global.config.properties.GoogleOAuthProperties;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class GoogleCalendarOAuthClient {

	private static final String AUTHORIZATION_URL = "https://accounts.google.com/o/oauth2/v2/auth";
	private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

	private final GoogleOAuthProperties googleOAuthProperties;
	private final GoogleCalendarProperties googleCalendarProperties;
	private final RestClient.Builder restClientBuilder;

	/* 구글 캘린더 OAuth 인증 URL 생성 (연동)*/
	public String buildAuthorizationUrl(String state) {
		String clientId = resolveClientId();

		return UriComponentsBuilder.fromUriString(AUTHORIZATION_URL)
			.queryParam("client_id", clientId)
			.queryParam("redirect_uri", googleCalendarProperties.redirectUri())
			.queryParam("response_type", "code")
			.queryParam("scope", googleCalendarProperties.scope())
			.queryParam("access_type", "offline")
			.queryParam("prompt", "consent")
			.queryParam("state", state)
			.build()
			.encode()
			.toUriString();
	}


	/* 구글 캘린더 OAuth 토큰 교환 (콜백)*/
	public GoogleOAuthTokenResponse exchangeAuthorizationCode(String code) {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("code", code);
		formData.add("client_id", resolveClientId());
		formData.add("client_secret", resolveClientSecret());
		formData.add("redirect_uri", googleCalendarProperties.redirectUri());
		formData.add("grant_type", "authorization_code");

		// 요청 시도
		try {
			return restClientBuilder.build()
				.post()
				.uri(TOKEN_URL)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(formData)
				.retrieve()
				.body(GoogleOAuthTokenResponse.class);
		} catch (RestClientException exception) {
			throw new BootSignalException(ErrorCode.CALENDAR_OAUTH_EXCHANGE_FAILED);
		}
	}

	// 구글 클라이언트 ID 해석
	private String resolveClientId() {
		if (!StringUtils.hasText(googleOAuthProperties.clientId())) {
			throw new BootSignalException(ErrorCode.INTERNAL_SERVER_ERROR, "Google Client ID 설정이 필요합니다.");
		}
		return googleOAuthProperties.clientId().strip();
	}

	// 구글 클라이언트 시크릿 해석
	private String resolveClientSecret() {
		if (!StringUtils.hasText(googleOAuthProperties.clientSecret())) {
			throw new BootSignalException(ErrorCode.INTERNAL_SERVER_ERROR, "Google Client Secret 설정이 필요합니다.");
		}
		return googleOAuthProperties.clientSecret().strip();
	}
}
