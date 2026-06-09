package com.bootsignal.global.exception;

public class BootSignalException extends RuntimeException {

	private final ErrorCode errorCode;

	public BootSignalException(ErrorCode errorCode) {
		super(errorCode.message());
		this.errorCode = errorCode;
	}

	public BootSignalException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public BootSignalException(ErrorCode errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	public ErrorCode errorCode() {
		return errorCode;
	}
}
