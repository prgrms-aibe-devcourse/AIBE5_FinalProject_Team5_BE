package com.bootsignal.domain.user.dto;

/**
 * 로그인 사용자 계정 작업의 성공 여부를 반환하는 DTO입니다.
 */
public record MemberActionResponse(
	boolean completed
) {

	public static MemberActionResponse success() {
		return new MemberActionResponse(true);
	}
}
