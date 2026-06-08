package com.bootsignal.domain.auth.oauth;

public record GoogleUserInfo(
	String subject,
	String email,
	String name,
	String pictureUrl
) {
}
