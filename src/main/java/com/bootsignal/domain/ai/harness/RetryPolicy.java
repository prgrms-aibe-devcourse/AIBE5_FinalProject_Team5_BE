package com.bootsignal.domain.ai.harness;

import com.bootsignal.domain.ai.exception.AiAgentException;
import com.bootsignal.domain.ai.exception.AiNonRetryableException;
import com.bootsignal.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class RetryPolicy {

	private static final int DEFAULT_MAX_ATTEMPTS = 2;
	private static final long DEFAULT_BACKOFF_MILLIS = 0L;

	private final int maxAttempts;
	private final long backoffMillis;

	public RetryPolicy() {
		this(DEFAULT_MAX_ATTEMPTS, DEFAULT_BACKOFF_MILLIS);
	}

	private RetryPolicy(int maxAttempts, long backoffMillis) {
		if (maxAttempts < 1) {
			throw new IllegalArgumentException("AI Agent 최대 시도 횟수는 1 이상이어야 합니다.");
		}
		if (backoffMillis < 0) {
			throw new IllegalArgumentException("AI Agent 재시도 대기 시간은 0 이상이어야 합니다.");
		}
		this.maxAttempts = maxAttempts;
		this.backoffMillis = backoffMillis;
	}

	public static RetryPolicy of(int maxAttempts, long backoffMillis) {
		return new RetryPolicy(maxAttempts, backoffMillis);
	}

	public static RetryPolicy noRetry() {
		return new RetryPolicy(1, 0);
	}

	public boolean canRetry(RuntimeException exception, int attempt) {
		// OpenAI 일시 장애나 파싱 실패처럼 회복 가능한 예외만 재시도한다.
		return attempt < maxAttempts
			&& isRetryableException(exception);
	}

	public boolean isRetryableException(RuntimeException exception) {
		return exception instanceof AiAgentException aiAgentException
			&& aiAgentException.isRetryable();
	}

	public AiNonRetryableException exhausted(RuntimeException exception) {
		return new AiNonRetryableException(
			ErrorCode.AI_RETRY_EXHAUSTED,
			"AI Agent 재시도 횟수를 초과했습니다.",
			exception
		);
	}

	public void waitBeforeRetry() {
		if (backoffMillis == 0) {
			return;
		}
		try {
			Thread.sleep(backoffMillis);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new AiNonRetryableException(
				ErrorCode.AI_EXECUTION_FAILED,
				"AI Agent 재시도 대기 중 인터럽트되었습니다.",
				exception
			);
		}
	}
}
