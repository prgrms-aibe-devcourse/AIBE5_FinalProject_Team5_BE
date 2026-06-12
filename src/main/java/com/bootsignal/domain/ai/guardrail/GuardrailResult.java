package com.bootsignal.domain.ai.guardrail;

import com.bootsignal.domain.ai.exception.AiAgentException;
import com.bootsignal.domain.ai.exception.AiNonRetryableException;
import com.bootsignal.domain.ai.exception.AiRetryableException;
import com.bootsignal.global.exception.ErrorCode;
import org.springframework.util.StringUtils;

public record GuardrailResult(
	boolean valid,
	ErrorCode errorCode,
	String message,
	boolean retryable
) {

	public GuardrailResult {
		// Guardrail 실패 결과는 Harness에서 재시도 가능 여부에 맞는 AI 예외로 바뀐다.
		if (valid) {
			errorCode = null;
			message = "";
			retryable = false;
		} else {
			if (errorCode == null) {
				throw new IllegalArgumentException("Guardrail 실패 ErrorCode는 필수입니다.");
			}
			message = StringUtils.hasText(message) ? message.strip() : errorCode.message();
		}
	}

	public static GuardrailResult pass() {
		return new GuardrailResult(true, null, null, false);
	}

	public static GuardrailResult fail(ErrorCode errorCode, String message) {
		return new GuardrailResult(false, errorCode, message, false);
	}

	public static GuardrailResult retryableFail(ErrorCode errorCode, String message) {
		return new GuardrailResult(false, errorCode, message, true);
	}

	public AiAgentException toException() {
		if (valid) {
			throw new IllegalStateException("통과한 Guardrail 결과는 예외로 변환할 수 없습니다.");
		}
		if (retryable) {
			return new AiRetryableException(errorCode, message);
		}
		return new AiNonRetryableException(errorCode, message);
	}
}
