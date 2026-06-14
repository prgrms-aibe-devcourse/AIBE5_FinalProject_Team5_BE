package com.bootsignal.domain.ai.exception;

import com.bootsignal.global.exception.ErrorCode;

public class AiRetryableException extends AiAgentException {

	public AiRetryableException(ErrorCode errorCode, String message) {
		super(errorCode, message, true);
	}

	public AiRetryableException(ErrorCode errorCode, String message, Throwable cause) {
		super(errorCode, message, true, cause);
	}
}
