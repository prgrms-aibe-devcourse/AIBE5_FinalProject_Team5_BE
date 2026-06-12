package com.bootsignal.domain.ai.exception;

import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;

public class AiAgentException extends BootSignalException {

	// 입력 오류는 재시도하지 않고, 일시 장애나 파싱 실패만 재시도 대상으로 표시한다.
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
