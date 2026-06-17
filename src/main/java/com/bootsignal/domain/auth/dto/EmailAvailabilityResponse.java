package com.bootsignal.domain.auth.dto;

/**
 * 이메일 중복 확인 결과를 프론트에서 바로 판단할 수 있도록 반환하는 DTO입니다.
 */
public record EmailAvailabilityResponse(
	String email,
	boolean available
) {
}
