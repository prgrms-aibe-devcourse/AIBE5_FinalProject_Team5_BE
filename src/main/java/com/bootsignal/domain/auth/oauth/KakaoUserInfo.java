package com.bootsignal.domain.auth.oauth;

public record KakaoUserInfo(
	String subject,
	String email,
	String nickname,
	String pictureUrl
) {
}
