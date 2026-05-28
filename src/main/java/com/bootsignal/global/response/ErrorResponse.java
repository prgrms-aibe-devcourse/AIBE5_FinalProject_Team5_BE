package com.bootsignal.global.response;

import com.bootsignal.global.exception.ErrorCode;
import java.util.List;

public record ErrorResponse(
	String code,
	String message,
	List<FieldError> fieldErrors
) {

	public ErrorResponse {
		fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
	}

	public static ErrorResponse of(ErrorCode errorCode) {
		return of(errorCode, errorCode.message());
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return of(errorCode, message, List.of());
	}

	public static ErrorResponse of(ErrorCode errorCode, String message, List<FieldError> fieldErrors) {
		return new ErrorResponse(errorCode.code(), message, fieldErrors);
	}

	public record FieldError(
		String field,
		String message
	) {
	}
}
