package com.bootsignal.global.response;

/**
 * 모든 API 응답을 프론트 공통 계약(success/code/message/data/error)에 맞춰 감싸는 표준 응답입니다.
 */
public record ApiResponse<T>(
	boolean success,
	String code,
	String message,
	T data,
	ErrorResponse error
) {

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, "SUCCESS", "요청이 성공했습니다.", data, null);
	}

	public static ApiResponse<Void> failure(ErrorResponse error) {
		return new ApiResponse<>(false, error.code(), error.message(), null, error);
	}
}
