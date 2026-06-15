package com.bootsignal.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
	BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
	VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "요청 값이 올바르지 않습니다."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
	COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "과정을 찾을 수 없습니다."),
	INSTITUTION_NOT_FOUND(HttpStatus.NOT_FOUND, "INSTITUTION_NOT_FOUND", "훈련기관을 찾을 수 없습니다."),
	POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다."),
	COURSE_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE_SESSION_NOT_FOUND", "훈련과정 회차를 찾을 수 없습니다."),
	REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", "리뷰를 찾을 수 없습니다."),
	REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "신고를 찾을 수 없습니다."),
	REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS", "이미 해당 회차에 리뷰를 작성하셨습니다."),
	VERIFICATION_NOT_APPROVED(HttpStatus.FORBIDDEN, "VERIFICATION_NOT_APPROVED", "수료 인증이 승인되지 않아 인증 리뷰를 작성할 수 없습니다."),
	REVIEW_TYPE_DOWNGRADE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "REVIEW_TYPE_DOWNGRADE_NOT_ALLOWED", "인증 리뷰를 일반 리뷰로 변경할 수 없습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다."),
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 미디어 타입입니다."),
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 가입된 이메일입니다."),
	DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다."),
	INVALID_LOGIN_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_LOGIN_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."),
	INVALID_OAUTH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_OAUTH_TOKEN", "유효하지 않은 소셜 로그인 토큰입니다."),
	OAUTH_PROVIDER_MISMATCH(HttpStatus.CONFLICT, "OAUTH_PROVIDER_MISMATCH", "이미 다른 로그인 방식으로 가입된 이메일입니다."),
	CALENDAR_GOOGLE_USER_ONLY(HttpStatus.FORBIDDEN, "CALENDAR_GOOGLE_USER_ONLY", "Google Calendar 연동은 Google 로그인 사용자만 이용할 수 있습니다."),
	CALENDAR_OAUTH_STATE_INVALID(HttpStatus.BAD_REQUEST, "CALENDAR_OAUTH_STATE_INVALID", "유효하지 않은 캘린더 연동 요청입니다."),
	CALENDAR_OAUTH_EXCHANGE_FAILED(HttpStatus.BAD_REQUEST, "CALENDAR_OAUTH_EXCHANGE_FAILED", "Google Calendar 토큰 발급에 실패했습니다."),
	CALENDAR_ALREADY_CONNECTED(HttpStatus.CONFLICT, "CALENDAR_ALREADY_CONNECTED", "이미 Google Calendar에 연결되어 있습니다."),
	CALENDAR_NOT_CONNECTED(HttpStatus.BAD_REQUEST, "CALENDAR_NOT_CONNECTED", "Google Calendar 연동 상태가 아닙니다."),
	AI_AGENT_NOT_FOUND(HttpStatus.BAD_REQUEST, "AI_AGENT_NOT_FOUND", "실행할 AI Agent를 찾을 수 없습니다."),
	AI_INPUT_INVALID(HttpStatus.BAD_REQUEST, "AI_INPUT_INVALID", "AI Agent 입력 값이 올바르지 않습니다."),
	AI_OUTPUT_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "AI_OUTPUT_INVALID", "AI Agent 출력 값이 올바르지 않습니다."),
	AI_RETRY_EXHAUSTED(HttpStatus.INTERNAL_SERVER_ERROR, "AI_RETRY_EXHAUSTED", "AI Agent 재시도 횟수를 초과했습니다."),
	AI_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI_EXECUTION_FAILED", "AI Agent 실행 중 오류가 발생했습니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;

	ErrorCode(HttpStatus status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}

	public HttpStatus status() {
		return status;
	}

	public String code() {
		return code;
	}

	public String message() {
		return message;
	}
}
