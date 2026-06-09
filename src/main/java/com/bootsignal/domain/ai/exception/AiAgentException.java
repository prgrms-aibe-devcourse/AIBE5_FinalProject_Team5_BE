package com.bootsignal.domain.ai.exception;

import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;

public class AiAgentException extends BootSignalException {

	private final boolean retryable;

	public AiAgentException(ErrorCode errorCode, String message, boolean retryable) {
		super(errorCode, message);
		this.retryable = retryable;
	}

	public AiAgentException(ErrorCode errorCode, String message, boolean retryable, Throwable cause) {
		super(errorCode, message, cause);
		this.retryable = retryable;
	}

	public boolean isRetryable() {
		return retryable;
	}
}
