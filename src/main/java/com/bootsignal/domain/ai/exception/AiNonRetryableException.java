package com.bootsignal.domain.ai.exception;

import com.bootsignal.global.exception.ErrorCode;

public class AiNonRetryableException extends AiAgentException {

	public AiNonRetryableException(ErrorCode errorCode, String message) {
		super(errorCode, message, false);
	}

	public AiNonRetryableException(ErrorCode errorCode, String message, Throwable cause) {
		super(errorCode, message, false, cause);
	}
}
