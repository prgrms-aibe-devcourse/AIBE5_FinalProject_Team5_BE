package com.bootsignal.domain.auth.oauth;

public interface GoogleTokenVerifier {

	GoogleUserInfo verify(String idToken);
}
