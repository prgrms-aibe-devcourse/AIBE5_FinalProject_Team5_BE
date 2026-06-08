package com.bootsignal.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
	@NotBlank(message = "Kakao ID Token은 필수입니다.")
	String idToken
) {
}
