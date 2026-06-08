package com.bootsignal.domain.auth.oauth;

public interface KakaoTokenVerifier {

	KakaoUserInfo verify(String idToken);
}
