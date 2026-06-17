package com.bootsignal.domain.auth.dto;

/**
 * 비밀번호 재설정처럼 단일 계정 작업의 성공 여부를 반환하는 공통 DTO입니다.
 */
public record AuthActionResponse(
	boolean completed
) {

	public static AuthActionResponse success() {
		return new AuthActionResponse(true);
	}
}
